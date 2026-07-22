import time
import requests
from flask import current_app
from app.utils.logger import logger


class DeepSeekClient:
    def __init__(self, app=None):
        self.app = app
        if app is not None:
            self.init_app(app)

    def init_app(self, app):
        self.app = app

    def chat(self, prompt, system_prompt=None):
        if self.app is None:
            self.app = current_app._get_current_object()

        api_url = self.app.config['DEEPSEEK_API_URL']
        api_key = self.app.config['DEEPSEEK_API_KEY']
        model_id = self.app.config['DEEPSEEK_MODEL_ID']
        timeout = self.app.config.get('REQUEST_TIMEOUT', 30)
        proxy = self.app.config.get('HTTP_PROXY', '')
        proxies = {'http': proxy, 'https': proxy} if proxy else None

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": model_id,
            "messages": messages,
            "max_tokens": 4096,
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        }

        max_retries = 2
        for attempt in range(max_retries + 1):
            start_time = time.time()
            try:
                logger.info(f"DeepSeek request: prompt_len={len(prompt)}, attempt={attempt + 1}")
                response = requests.post(
                    api_url,
                    json=payload,
                    headers=headers,
                    timeout=timeout,
                    proxies=proxies,
                )
                response.raise_for_status()
                result = response.json()

                message = result.get('choices', [{}])[0].get('message', {})
                content = message.get('content', '')
                if not content:
                    content = message.get('reasoning_content', '')
                elapsed = time.time() - start_time
                logger.info(f"DeepSeek response: len={len(content)}, elapsed={elapsed:.2f}s")
                return content

            except requests.exceptions.Timeout:
                elapsed = time.time() - start_time
                logger.warning(f"DeepSeek timeout: attempt={attempt + 1}, elapsed={elapsed:.2f}s")
                if attempt < max_retries:
                    wait = 2 ** attempt
                    time.sleep(wait)
                    continue
                raise Exception("AI服务超时，请稍后重试")

            except requests.exceptions.ConnectionError:
                logger.warning(f"DeepSeek connection error: attempt={attempt + 1}")
                if attempt < max_retries:
                    wait = 2 ** attempt
                    time.sleep(wait)
                    continue
                raise Exception("AI服务连接失败，请稍后重试")

            except requests.exceptions.HTTPError as e:
                elapsed = time.time() - start_time
                logger.error(f"DeepSeek HTTP error: {e.response.status_code}, elapsed={elapsed:.2f}s")
                if e.response.status_code == 401:
                    raise Exception("AI服务认证失败")
                if e.response.status_code >= 500 and attempt < max_retries:
                    wait = 2 ** attempt
                    time.sleep(wait)
                    continue
                raise Exception("AI服务不可用，请稍后重试")

            except Exception as e:
                logger.error(f"DeepSeek unexpected error: {str(e)}")
                raise Exception("AI服务异常，请稍后重试")

        raise Exception("AI服务不可用，请稍后重试")


deepseek_client = DeepSeekClient()
