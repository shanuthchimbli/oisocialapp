FROM golang:1.22-alpine AS builder

WORKDIR /app
COPY go.mod ./
# RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/oi-server ./cmd/server/main.go

FROM alpine:latest
RUN apk --no-cache add ca-certificates tzdata
WORKDIR /root/
COPY --from=builder /app/oi-server .

EXPOSE 8080
CMD ["./oi-server"]
