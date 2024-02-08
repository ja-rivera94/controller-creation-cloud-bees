#####################################################################################
#  Script that ingests data from YAML or JSON variables and renders the configuration
#  with a configuration template. Example
#
#  
#####################################################################################
import shutil
import sys
import yaml
import jinja2
import os.path
from pathlib import Path
import json


def main():
    template_path = sys.argv[1]
    output_path = sys.argv[2]
    template_data = json.loads(sys.argv[3])

    render(template_path, output_path, template_data)

    return

def render(template_file: str, output_path: str, template_data: dict):

    # Get the template data from file
    with open(Path(template_file), "r") as f:
        template_content = f.read()

    # Generate template object
    template = jinja2.Template(template_content)

    # Render the template
    output_content = template.render(template_data)

    # Save the configuration to output file
    with open(output_path, "w") as f:
        f.write(output_content)
    return

if __name__ == "__main__":
    main()