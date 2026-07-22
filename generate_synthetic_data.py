import os
import numpy as np
from PIL import Image

DISEASE_CLASSES = [
    "Tomato_Early_blight",
    "Tomato_Late_blight",
    "Tomato_Leaf_Mold",
    "Tomato_Septoria_leaf_spot",
    "Tomato_Spider_mites",
    "Tomato_Target_Spot",
    "Tomato_Yellow_Leaf_Curl_Virus",
    "Tomato_Mosaic_Virus",
    "Tomato_Healthy",
    "Potato_Early_blight"
]

NUM_IMAGES_PER_CLASS = 150
IMAGE_SIZE = 224


def create_leaf_mask(size=IMAGE_SIZE, shape_type="round"):
    y, x = np.ogrid[:size, :size]
    cx, cy = size // 2, size // 2
    
    if shape_type == "round":
        dist = np.sqrt((x - cx)**2 + (y - cy)**2)
        mask = dist < size / 2.2
    elif shape_type == "oval":
        dist = (x - cx)**2 / (size/3)**2 + (y - cy)**2 / (size/2.5)**2
        mask = dist < 1
    else:
        angle = np.arctan2(y - cy, x - cx)
        r = np.sqrt((x - cx)**2 + (y - cy)**2)
        leaf_shape = r < size / 2.5 * (0.8 + 0.4 * np.cos(angle * 3))
        mask = leaf_shape
    
    return mask.astype(bool)


def add_veins_fast(img, mask):
    y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
    cx, cy = IMAGE_SIZE // 2, IMAGE_SIZE // 2
    
    main_vein = np.abs(x - cx) < 4
    side_veins = np.zeros_like(mask, dtype=bool)
    
    for i in range(4):
        angle = (i - 1.5) * 0.4
        dx = (x - cx) * np.cos(angle) + (y - cy) * np.sin(angle)
        dy = -(x - cx) * np.sin(angle) + (y - cy) * np.cos(angle)
        vein = (dy > -IMAGE_SIZE/4) & (dy < IMAGE_SIZE/4) & (np.abs(dx) < 3)
        side_veins |= vein
    
    vein_mask = (main_vein | side_veins) & mask
    vein_color = np.array([60, 100, 40], dtype=np.uint8)
    img[vein_mask] = vein_color
    return img


def generate_healthy_leaf():
    shape_types = ["round", "oval", "lobed"]
    mask = create_leaf_mask(shape_type=np.random.choice(shape_types))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(100, 160), 
                           np.random.randint(140, 200), 
                           np.random.randint(70, 130)])
    
    noise = np.random.normal(0, 15, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_early_blight():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(80, 140), 
                           np.random.randint(120, 180), 
                           np.random.randint(60, 110)])
    
    noise = np.random.normal(0, 12, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    num_lesions = np.random.randint(3, 7)
    for _ in range(num_lesions):
        cx = np.random.randint(40, IMAGE_SIZE - 40)
        cy = np.random.randint(40, IMAGE_SIZE - 40)
        radius = np.random.randint(15, 35)
        
        y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
        dist = np.sqrt((x - cx)**2 + (y - cy)**2)
        lesion_mask = (dist < radius) & mask
        
        edge_mask = (dist < radius) & (dist > radius * 0.6) & mask
        edge_color = np.array([30, 30, 30])
        img[edge_mask] = edge_color
        
        inner_mask = (dist <= radius * 0.6) & mask
        inner_dist = dist[inner_mask]
        gray_values = ((inner_dist / (radius * 0.6)) * 120 + 60).astype(np.uint8)
        img[inner_mask] = np.stack([gray_values] * 3, axis=-1)
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_late_blight():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(80, 140), 
                           np.random.randint(120, 180), 
                           np.random.randint(60, 110)])
    
    noise = np.random.normal(0, 15, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    num_lesions = np.random.randint(2, 4)
    for _ in range(num_lesions):
        cx = np.random.randint(30, IMAGE_SIZE - 30)
        cy = np.random.randint(30, IMAGE_SIZE - 30)
        radius = np.random.randint(30, 60)
        
        y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
        dist = np.sqrt((x - cx)**2 + (y - cy)**2)
        lesion_mask = (dist < radius) & mask
        
        intensity = 1 - dist[lesion_mask] / radius
        r = (200 * intensity + img[lesion_mask, 0] * (1 - intensity) * 0.4).astype(np.uint8)
        g = (60 * intensity + img[lesion_mask, 1] * (1 - intensity) * 0.4).astype(np.uint8)
        b = (60 * intensity + img[lesion_mask, 2] * (1 - intensity) * 0.4).astype(np.uint8)
        
        img[lesion_mask] = np.stack([r, g, b], axis=-1)
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_leaf_mold():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(90, 150), 
                           np.random.randint(130, 190), 
                           np.random.randint(70, 120)])
    
    noise = np.random.normal(0, 10, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    mold_pattern = np.random.random((IMAGE_SIZE, IMAGE_SIZE)) < 0.15
    mold_mask = mold_pattern & mask
    
    mold_color = np.stack([
        np.random.randint(90, 140, IMAGE_SIZE * IMAGE_SIZE).reshape(IMAGE_SIZE, IMAGE_SIZE),
        np.random.randint(70, 120, IMAGE_SIZE * IMAGE_SIZE).reshape(IMAGE_SIZE, IMAGE_SIZE),
        np.random.randint(80, 130, IMAGE_SIZE * IMAGE_SIZE).reshape(IMAGE_SIZE, IMAGE_SIZE)
    ], axis=-1)
    
    img[mold_mask] = mold_color[mold_mask]
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_septoria_spot():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(90, 150), 
                           np.random.randint(130, 190), 
                           np.random.randint(70, 120)])
    
    noise = np.random.normal(0, 12, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    num_spots = np.random.randint(20, 40)
    for _ in range(num_spots):
        cx = np.random.randint(25, IMAGE_SIZE - 25)
        cy = np.random.randint(25, IMAGE_SIZE - 25)
        radius = np.random.randint(3, 8)
        
        y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
        dist = np.sqrt((x - cx)**2 + (y - cy)**2)
        
        spot_mask = (dist < radius) & mask
        img[spot_mask] = [np.random.randint(30, 80), 
                          np.random.randint(30, 80), 
                          np.random.randint(30, 80)]
        
        halo_mask = (dist >= radius) & (dist < radius + 4) & mask
        img[halo_mask] = np.clip(img[halo_mask] + [20, 20, 0], 0, 255)
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_spider_mites():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(80, 140), 
                           np.random.randint(120, 180), 
                           np.random.randint(60, 110)])
    
    noise = np.random.normal(0, 18, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    mite_pattern = np.random.random((IMAGE_SIZE, IMAGE_SIZE)) < 0.12
    mite_mask = mite_pattern & mask
    img[mite_mask] = [np.random.randint(150, 210), 
                      np.random.randint(150, 210), 
                      np.random.randint(150, 210)]
    
    web_pattern = np.random.random((IMAGE_SIZE, IMAGE_SIZE)) < 0.05
    web_mask = web_pattern & mask
    img[web_mask] = [220, 220, 220]
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_target_spot():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(80, 140), 
                           np.random.randint(120, 180), 
                           np.random.randint(60, 110)])
    
    noise = np.random.normal(0, 10, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    num_targets = np.random.randint(2, 5)
    for _ in range(num_targets):
        cx = np.random.randint(40, IMAGE_SIZE - 40)
        cy = np.random.randint(40, IMAGE_SIZE - 40)
        
        y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
        
        for r, color in [(35, [100, 60, 60]), (22, [80, 40, 40]), (10, [60, 20, 20])]:
            dist = np.sqrt((x - cx)**2 + (y - cy)**2)
            ring_mask = (dist >= r - 4) & (dist < r) & mask
            img[ring_mask] = color
        
        center_dist = np.sqrt((x - cx)**2 + (y - cy)**2)
        center_mask = (center_dist < 5) & mask
        img[center_mask] = [120, 80, 80]
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_yellow_curl():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(170, 240), 
                           np.random.randint(150, 210), 
                           np.random.randint(30, 80)])
    
    noise = np.random.normal(0, 25, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    yellow_pattern = np.random.random((IMAGE_SIZE, IMAGE_SIZE)) < 0.2
    yellow_mask = yellow_pattern & mask
    img[yellow_mask] = [np.random.randint(190, 255), 
                        np.random.randint(170, 230), 
                        np.random.randint(20, 70)]
    
    vein_color = (np.random.randint(120, 160), np.random.randint(140, 180), np.random.randint(40, 80))
    y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
    cx, cy = IMAGE_SIZE // 2, IMAGE_SIZE // 2
    
    main_vein = np.abs(x - cx) < 4
    side_veins = np.zeros_like(mask, dtype=bool)
    for i in range(4):
        angle = (i - 1.5) * 0.4
        dx = (x - cx) * np.cos(angle) + (y - cy) * np.sin(angle)
        dy = -(x - cx) * np.sin(angle) + (y - cy) * np.cos(angle)
        vein = (dy > -IMAGE_SIZE/4) & (dy < IMAGE_SIZE/4) & (np.abs(dx) < 3)
        side_veins |= vein
    
    vein_mask = (main_vein | side_veins) & mask
    img[vein_mask] = vein_color
    
    return Image.fromarray(img)


def generate_mosaic_virus():
    mask = create_leaf_mask(shape_type=np.random.choice(["round", "oval"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(90, 150), 
                           np.random.randint(130, 190), 
                           np.random.randint(70, 120)])
    
    noise = np.random.normal(0, 12, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    cell_size = np.random.randint(12, 22)
    cell_y, cell_x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
    cell_grid = ((cell_y // cell_size) + (cell_x // cell_size)) % 2
    
    bright_mask = (cell_grid == 0) & mask
    dark_mask = (cell_grid == 1) & mask
    
    img[bright_mask] = np.clip(img[bright_mask] + np.random.randint(30, 70), 0, 255)
    img[dark_mask] = np.clip(img[dark_mask] - np.random.randint(10, 40), 0, 255)
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


def generate_potato_early_blight():
    mask = create_leaf_mask(shape_type=np.random.choice(["oval", "lobed"]))
    
    img = np.ones((IMAGE_SIZE, IMAGE_SIZE, 3), dtype=np.uint8) * 255
    base_color = np.array([np.random.randint(70, 130), 
                           np.random.randint(110, 170), 
                           np.random.randint(50, 100)])
    
    noise = np.random.normal(0, 15, (IMAGE_SIZE, IMAGE_SIZE, 3)).astype(np.int16)
    leaf_color = np.clip(base_color + noise, 0, 255).astype(np.uint8)
    img[mask] = leaf_color[mask]
    
    num_lesions = np.random.randint(5, 12)
    for _ in range(num_lesions):
        cx = np.random.randint(35, IMAGE_SIZE - 35)
        cy = np.random.randint(35, IMAGE_SIZE - 35)
        radius = np.random.randint(12, 28)
        
        y, x = np.ogrid[:IMAGE_SIZE, :IMAGE_SIZE]
        dist = np.sqrt((x - cx)**2 + (y - cy)**2)
        angle = np.arctan2(y - cy, x - cx)
        
        lesion_mask = (dist < radius) & mask
        radial_pattern = np.sin(angle * 8 + dist * 0.4)
        intensity = 0.3 + 0.4 * radial_pattern
        gray = (40 + intensity * 90).astype(np.uint8)
        
        img[lesion_mask] = np.stack([gray[lesion_mask]] * 3, axis=-1)
    
    img = add_veins_fast(img, mask)
    return Image.fromarray(img)


CLASS_GENERATORS = {
    "Tomato_Early_blight": generate_early_blight,
    "Tomato_Late_blight": generate_late_blight,
    "Tomato_Leaf_Mold": generate_leaf_mold,
    "Tomato_Septoria_leaf_spot": generate_septoria_spot,
    "Tomato_Spider_mites": generate_spider_mites,
    "Tomato_Target_Spot": generate_target_spot,
    "Tomato_Yellow_Leaf_Curl_Virus": generate_yellow_curl,
    "Tomato_Mosaic_Virus": generate_mosaic_virus,
    "Tomato_Healthy": generate_healthy_leaf,
    "Potato_Early_blight": generate_potato_early_blight
}


def generate_dataset(output_dir="data"):
    os.makedirs(output_dir, exist_ok=True)
    
    for disease_class in DISEASE_CLASSES:
        class_dir = os.path.join(output_dir, disease_class)
        os.makedirs(class_dir, exist_ok=True)
        
        for f in os.listdir(class_dir):
            os.remove(os.path.join(class_dir, f))
        
        generator = CLASS_GENERATORS[disease_class]
        
        for i in range(NUM_IMAGES_PER_CLASS):
            img = generator()
            img_path = os.path.join(class_dir, f"{disease_class}_{i:04d}.jpg")
            img.save(img_path)
        
        print(f"Generated {NUM_IMAGES_PER_CLASS} images for {disease_class}")
    
    total_images = len(DISEASE_CLASSES) * NUM_IMAGES_PER_CLASS
    print(f"\nTotal images generated: {total_images}")
    print(f"Output directory: {os.path.abspath(output_dir)}")


if __name__ == "__main__":
    generate_dataset()