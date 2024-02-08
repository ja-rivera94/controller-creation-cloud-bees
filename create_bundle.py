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


def main():
    # We will be using the sys.argv method to collect the arguments passed
    # Also to have the values as proper systems Paths and not worry about termination
    # based on filesystem, we are leveraging the Path object from pathlib
    
    tenantname = sys.argv[1]
    bundle_name = sys.argv[2]
    sonarqube_edition = sys.argv[3]
    local_folder_cloudbees_repo = sys.argv[4]
    template_path = Path(f"../{local_folder_cloudbees_repo}/controller-creation/template")
    tenant_dir = ("./" + bundle_name)

    # evaluating sonarqube edition
    sonarQube_community_edition = "http://sonarqube.sonarqube-community.svc.cluster.local:33333"
    sonarQube_enterprise_edition = "http://sonarqube.sonarqube.svc.cluster.local:33333"
    if sonarqube_edition == "SonarQube_Community_Edition":
        sonarqube_edition = sonarQube_community_edition
    else:
        sonarqube_edition = sonarQube_enterprise_edition
    
    create_folder(tenant_dir, tenantname)

    for files in os.listdir(template_path):
        if files.endswith(".j2"):
            filename = str(template_path) + "/" + files
            template_file = Path(filename)
            render(template_file, tenant_dir, tenantname, sonarqube_edition, bundle_name)
        elif files.endswith(".yaml"):
            src_path = str(template_path) + "/" + files
            dst_path = str(tenant_dir) + "/" + files  
            shutil.copyfile(src_path, dst_path) 
    return

def create_folder(tenant_dir, tenantname):
    # create a new dir with team name as foldername
    try:
        os.mkdir(tenant_dir)
    except OSError as e:
            print(e)
            print("Unable to create a new controller with name: " + tenantname + " as bundle files already exists")

def render(template_file, tenant_dir, tenantname, sonarqube_edition, bundle_name):

    # Verify template format
    if template_file.suffix != ".j2":
        sys.exit(f"Template file format not supported: {template_file.suffix}")

    # Get the template data from file
    with open(template_file, "r") as f:
        template_data = f.read()

    # Generate template object
    template = jinja2.Template(template_data)
    # Placing variables as per .j2 files

    # Render the template
    configuration_data = template.render(sonarqube_edition=sonarqube_edition, tenant_name=tenantname, bundle_name=bundle_name)

    # Save the configuration to output file
    template_file = template_file.with_suffix('')
    output_file = os.path.basename(template_file)
    output_path = os.path.join(tenant_dir, output_file)
    with open(output_path, "w") as f:
        f.write(configuration_data)
    return

if __name__ == "__main__":
    main()