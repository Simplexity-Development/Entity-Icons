import os
import json
from PIL import Image


def create_sprite_sheet_with_metadata(folder_path, output_path, sprite_size):
    # List all image files in the folder
    image_files = [f for f in os.listdir(folder_path) if f.endswith(('png', 'jpg', 'jpeg', 'bmp', 'gif'))]
    print(f"Found {len(image_files)} image files.")

    # Load images
    images = [Image.open(os.path.join(folder_path, file)) for file in image_files]

    # Calculate the size of the sprite sheet
    num_images = len(images)
    grid_size = int(num_images ** 0.5) + 1
    sprite_sheet_width = grid_size * sprite_size
    sprite_sheet_height = grid_size * sprite_size

    # Create a new blank image for the sprite sheet
    sprite_sheet = Image.new('RGBA', (sprite_sheet_width, sprite_sheet_height))

    # Initialize metadata dictionary
    metadata = {}

    # Define possible colors and variants
    colors = ["armorer", "butcher", "cleric", "farmer", "fisherman", "fletcher", "librarian", "shepherd",  "weaponsmith", "default"]
    variants = ["default", "swamp", "savanna", "desert", "tundra"]

    # Paste each image into the sprite sheet and generate metadata
    for index, image in enumerate(images):
        row = index // grid_size
        col = index % grid_size
        x = col * sprite_size
        y = row * sprite_size
        image = image.resize((sprite_size, sprite_size), Image.Resampling.LANCZOS)
        sprite_sheet.paste(image, (x, y))

        # Extract base and overlay colors from the file name
        file_name = os.path.basename(image_files[index])
        name_parts = file_name.replace('.png', '').split('_', 1)

        # Ensure there are exactly two parts (base and overlay colors)
        if len(name_parts) == 2:
            base_color, overlay_color = name_parts
            # Check if base_color is in variants and overlay_color is in colors
            if base_color in variants and overlay_color in colors:
                # Add entry to metadata
                metadata[file_name] = {
                    "position": {"x": x, "y": y},
                    "variant": base_color,
                    "decoration": overlay_color
                }
                print(f"Added metadata for {file_name}: {metadata[file_name]}")
            else:
                print(f"Skipped {file_name}: base_color or overlay_color not in variants or colors")
        else:
            print(f"Skipped {file_name}: name_parts length not 2")

    # Save the sprite sheet
    sprite_sheet.save(output_path)

    # Save metadata as JSON
    json_output_path = output_path.replace('.png', '.json')
    with open(json_output_path, 'w') as json_file:
        json.dump(metadata, json_file, indent=4)

    print(f"Metadata saved to {json_output_path}")


# Example usage
folder_path = r'D:\Documents\IdeaProjects\Entity-Icons\assets\combined\16x16\zombie_villager'
output_path = r'D:\Documents\IdeaProjects\Entity-Icons\assets\sprite_sheets\16x16\zombie_villager.png'
sprite_size = 16  # Size of each sprite in the sprite sheet

create_sprite_sheet_with_metadata(folder_path, output_path, sprite_size)
