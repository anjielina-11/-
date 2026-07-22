import subprocess
import sys
import time
import os

backend_dir = os.path.join(os.path.dirname(__file__), 'backend')
frontend_dir = os.path.join(os.path.dirname(__file__), 'frontend')

print("Starting MedSearch Backend...")
backend = subprocess.Popen(
    [sys.executable, '-c',
     "from app import create_app; app = create_app(); app.run(host='127.0.0.1', port=5000, debug=False)"],
    cwd=backend_dir
)

print("Waiting for backend...")
time.sleep(5)

print("Starting MedSearch Frontend...")
frontend = subprocess.Popen(
    ['npm', 'run', 'dev'],
    cwd=frontend_dir,
    shell=True
)

print()
print("=" * 50)
print("  MedSearch is running!")
print("  Backend:  http://localhost:5000")
print("  Frontend: http://localhost:5173")
print("=" * 50)
print()
print("Press Ctrl+C to stop both services...")

try:
    while True:
        if backend.poll() is not None:
            print("Backend process exited unexpectedly!")
            break
        if frontend.poll() is not None:
            print("Frontend process exited unexpectedly!")
            break
        time.sleep(1)
except KeyboardInterrupt:
    print("\nStopping services...")
finally:
    backend.terminate()
    frontend.terminate()
    backend.wait(timeout=5)
    frontend.wait(timeout=5)
    print("Services stopped.")
