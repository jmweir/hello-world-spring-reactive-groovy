## Spring Reactive Groovy Example

A demo app to show how to setup Reactive Spring on Netty, written in Groovy.

### Build Application Jar
```bash
$ ./gradlew build
```

### Build Docker Image
```bash
$ DOCKER_BUILDKIT=1 docker build -t com.example/hello-world:1.0.0 .
```

## Deploy to Local Kubernetes

### Install Kubernetes
1. Install Docker for Desktop from here: https://www.docker.com/products/docker-desktop
2. Activate Kubernetes in Docker preferences (takes 5-10 minutes the first time):
![Enable Kubernetes](images/EnableKubernetes.png)
3. Confirm Kubernetes installation:
```bash
$ kubectl cluster-info
Kubernetes master is running at https://kubernetes.docker.internal:6443
KubeDNS is running at https://kubernetes.docker.internal:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
```

### Create Kubernetes Deployment
1. Generate YAML:
```bash
$ mkdir .kube
$ kubectl create deployment hello-world --image=com.example/hello-world:1.0.0 --dry-run -o yaml >.kube/deployment.yaml
$ cat .kube/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  creationTimestamp: null
  labels:
    app: hello-world
  name: hello-world
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hello-world
  strategy: {}
  template:
    metadata:
      creationTimestamp: null
      labels:
        app: hello-world
    spec:
      containers:
      - image: com.example/hello-world:1.0.0
        name: hello-world
        resources: {}
status: {}
```
2. Apply deployment:
```bash
$ kubectl apply -f .kube/deployment.yaml
deployment.apps/hello-world created
```

### Forward Deployment Port and Test
```bash
$ kubectl port-forward deployment/hello-world 8080
Forwarding from 127.0.0.1:8080 -> 8080
Forwarding from [::1]:8080 -> 8080
```
Test the application by browsing to http://localhost:8080

### Scale the Deployment
1. Update replicas from 1 to 2 in `.kube/deployment.yaml`
2. Redeploy with: `kubectl apply -f .kube/deployment.yaml`

### Create Kubernetes Load Balancing Service
1. Generate YAML:
```bash
$ kubectl create service clusterip hello-world --tcp=8080:8080 --dry-run -o yaml >.kube/service.yaml
$ cat .kube/service.yaml
apiVersion: v1
kind: Service
metadata:
  creationTimestamp: null
  labels:
    app: hello-world
  name: hello-world
spec:
  ports:
  - name: 8080-8080
    port: 8080
    protocol: TCP
    targetPort: 8080
  selector:
    app: hello-world
  type: ClusterIP
status:
  loadBalancer: {}
```
2. Apply service:
```bash
$ kubectl apply -f .kube/service.yaml
service/hello-world created
```

### Forward Service Port and Test
```bash
$ kubectl port-forward svc/hello-world 8080
Forwarding from 127.0.0.1:8080 -> 8080
Forwarding from [::1]:8080 -> 8080
```
Test the application by browsing to http://localhost:8080

### Visualize POD Identification
1. Add `app.host: ${HOSTNAME}` to application.yaml
2. Add `String host` to Application.Config
3. Add `${host}` somewhere in the template

### Deploy a New Image and Confirm Balancing
1. Build a new Docker image: `DOCKER_BUILDKIT=1 docker build -t com.example/hello-world:1.0.1 .`
2. Update 1.0.0 to 1.0.1 in `.kube/deployment.yaml`
3. Redeploy with: `kubectl apply -f .kube/deployment.yaml`
4. Reload http://localhost:8080 a few times and confirm round-robin load balancing.

### Externalize configuration in a ConfigMap
1. Generate a ConfigMap:
```bash
$ kubectl create configmap hello-world --from-literal=app.name=Jim --dry-run -o yaml >.kube/config.yaml
$ cat .kube/config.yaml
apiVersion: v1
data:
  app.name: Jim
kind: ConfigMap
metadata:
  creationTimestamp: null
  name: hello-world
```
2. Apply the ConfigMap:
```bash
$ kubectl apply -f .kube/config.yaml
configmap/hello-world configured
```
3. Add `org.springframework.cloud:spring-cloud-starter-kubernetes-config:1.1.1.RELEASE' as a Gradle dependency
4. Remove `app.name` reference from `application.yaml`
5. Build a new Docker image and update the deployment

### Create Kubernetes NGINX Ingress
1. Install NGINX Ingress Controller (see: https://kubernetes.github.io/ingress-nginx/deploy):
```bash
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/nginx-0.29.0/deploy/static/mandatory.yaml
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/nginx-0.29.0/deploy/static/provider/cloud-generic.yaml
```
2. Create the file .kube/ingress.yaml:
```bash
$ cat .kube/ingress.yaml
apiVersion: networking.k8s.io/v1beta1
kind: Ingress
metadata:
  name: hello-world
spec:
  backend:
    serviceName: hello-world
    servicePort: 8080
```
3. Apply ingress:
```bash
$ kubectl apply -f .kube/ingress.yaml
ingress.networking.k8s.io/hello-world created
```

### Package with Helm
1. Generate a Helm chart:
```bash
$ helm create .kube/hello-world
Creating .kube/hello-world
```
2. Replace the contents of `templates` with the YAML files in `.kube`
3. Delete `values.yaml` because we will supply values at install time.
4. Externalize our name parameter. In `config.yaml` change `Victor` to `{{ .Values.Name }}`
5. Add NGINX as a chart dependency by adding the following to Chart.yaml:
```bash
dependencies:
  - name: nginx-ingress
    version: 1.31.0
    repository: https://kubernetes-charts.storage.googleapis.com/
```
6. Install NGINX chart with:
```bash
$ cd .kube/hello-world && helm dependency update
```
7. Delete all resources that we've previously installed:
```bash
$ kubectl delete ingress/hello-world configmap/hello-world svc/hello-world deploy/hello-world
$ kubectl delete ns/ingress-nginx
```
8. Install the new Helm chart:
```bash
$ helm install --set Name=Steve hello-world .kube/hello-world
```
