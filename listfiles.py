import os

def collect_files_content(root_dir, output_file):
    """
    Recursively read .kt and .xml files from root_dir and write their content to output_file.

    :param root_dir: Root directory of the Android project.
    :param output_file: Path to the output file.
    """
    # List of file extensions to include
    extensions = [".kt", ".xml"]

    with open(output_file, "w", encoding="utf-8") as outfile:
        for subdir, _, files in os.walk(root_dir):
            for file in files:
                if file.endswith(tuple(extensions)):
                    file_path = os.path.join(subdir, file)
                    outfile.write(f"===== File: {file_path} =====\n")
                    try:
                        with open(file_path, "r", encoding="utf-8") as infile:
                            outfile.write(infile.read())
                    except Exception as e:
                        outfile.write(f"Error reading file: {e}\n")
                    outfile.write("\n\n")

if __name__ == "__main__":
    # Change this to your Android project root directory
    project_root = "c:/xchat"
    # Output file
    output_file_path = "output.txt"

    collect_files_content(project_root, output_file_path)
    print(f"Content written to {output_file_path}")