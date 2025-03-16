import PIL
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
        base_image = Image.open(base_image_path).convert('RGBA')
        overlay_image = Image.open(overlay_image_path).convert('RGBA')

        # Ensure both images are processed
        base_width, base_height = base_image.size
        overlay_width, overlay_height = overlay_image.size

        # Calculate the position to center the overlay on the base
        offset_x = (base_width - overlay_width) // 2
        offset_y = (base_height - overlay_height) // 2

        # Create a new image to combine them
        combined_image = base_image.copy()
        combined_image.paste(overlay_image, (offset_x, offset_y), overlay_image)

        # Save the combined image
        combined_image_name = f'{os.path.splitext(base_image_name)[0]}_{os.path.splitext(overlay_image_name)[0]}.png'
        combined_image_path = os.path.join(output_folder, combined_image_name)
        combined_image.save(combined_image_path)

        print(f'Saved {combined_image_name}')