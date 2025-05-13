# Terraform

Go to terraform environment directory:

```sh
cd ./environments/local/
```

Create workspaces:

```sh
terraform workspace new cluster
terraform workspace new single
terraform workspace new client
```

> Workspaces with relevant VM deployment configurations:
> * `single` - `vms/single.tfvars` - 1 VM for deployment
> * `cluster` - `vms/cluster.tfvars` - 1 Manager and 3 Worker VMs for deployment
> * `client` - `vms/client.tfvars` - 1 VM for client testing

Create VMS:

```sh
terraform init
terraform workspace select <workspace>
terraform apply -auto-approve -var-file=<vms_tfvars>
```

> `vms_tfvars` - file with VM configuration, described above

Get ip addresses of VMs for ansible:

```sh
terraform output
```

Destroy VMs:

```sh
terraform destroy -auto-approve -var-file=<vms_tfvars>
```
