FROM node:20-alpine

WORKDIR /usr/src/app

COPY package*.json ./

RUN npm install

COPY . .

RUN mkdir -p dist/plant/data && \
    cp -r src/plant/data/* dist/plant/data/

EXPOSE 3000

CMD ["npm", "run", "start:dev"]
