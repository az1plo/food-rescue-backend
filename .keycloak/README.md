# Keycloak Docker image

- Pred spustením Keycloak v AKS (Azure Kubernetes service) je potrebné image zbuildiť a následne
  pushnúť do ACR (Azure Container registry).

```sh
# Priklad export z 1Password
# Pri exporte je potrebne za `=` dat presnu hodnotu (najlepsie v sigle-quotes)
export ACR=$(op read "op://FullStackAcademy/ACR/url")
export ACR_USER=$(op read "op://FullStackAcademy/ACR/username")
export ACR_PASSWORD=$(op read "op://FullStackAcademy/ACR/password")

make all
```
