from PIL import Image
import os

# Set your base and overlay folder paths
base_folder = 'D:\Documents\IdeaProjects\Entity-Icons\\assets\png_files\\16x16\\zombie_villager'
overlay_folder = 'D:\Documents\IdeaProjects\Entity-Icons\\assets\png_files\8x8'
output_folder = 'D:\Documents\IdeaProjects\Entity-Icons\\assets\combined\\16x16\\zombie_villager'

# Make sure the output folder exists
if not os.path.exists(output_folder):
    os.makedirs(output_folder)

# Get the list of all base and overlay images
base_images = [f for f in os.listdir(base_folder) if os.path.isfile(os.path.join(base_folder, f))]
overlay_images = [f for f in os.listdir(overlay_folder) if os.path.isfile(os.path.join(overlay_folder, f))]

# Loop through each combination of base and overlay
for base_image_name in base_images:
    for overlay_image_name in overlay_images:
        base_image_path = os.path.join(base_folder, base_image_name)
        overlay_image_path = os.path.join(overlay_folder, overlay_image_name)

        # Open the base and overlay images
        base_image = Image.open(base_image_path)
        overlay_image = Image.open(overlay_image_path)

        # Ensure both images have the same size
        if base_image.size != overlay_image.size:
            overlay_image = overlay_image.resize(base_image.size)

        # Combine the base and overlay images
        combined_image = Image.alpha_composite(base_image.convert('RGBA'), overlay_image.convert('RGBA'))

        # Save the combined image
        combined_image_name = f'{os.path.splitext(base_image_name)[0]}_{os.path.splitext(overlay_image_name)[0]}.png'
        combined_image_path = os.path.join(output_folder, combined_image_name)
        combined_image.save(combined_image_path)

        print(f'Saved {combined_image_name}')
