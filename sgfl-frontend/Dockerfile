# ==========================================
# Estágio 1: Build da aplicação React + Vite
# ==========================================
FROM node:22-alpine AS builder
WORKDIR /app

# Instala dependências aproveitando o cache do Docker
COPY package.json package-lock.json ./
RUN npm ci

# Variável de ambiente usada durante o build do Vite
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

# Copia código-fonte e executa o build de produção
COPY . .
RUN npm run build

# ==========================================
# Estágio 2: Servidor Web Nginx de produção
# ==========================================
FROM nginx:stable-alpine AS runner

# Substitui a configuração padrão do Nginx pela personalizada
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia o build estático gerado para o diretório padrão do Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
