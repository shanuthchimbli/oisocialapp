package main

import (
	"log"
	"net/http"
	"os"

	"chat.oi/backend/internal/gateway"
)

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	hub := gateway.NewHub()
	go hub.Run()

	// WebSocket Gateway Endpoint
	http.HandleFunc("/v1/ws", func(w http.ResponseWriter, r *http.Request) {
		gateway.ServeWs(hub, w, r)
	})

	// Health check endpoint for automated orchestrators (NFR-OPS-002)
	http.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"HEALTHY","service":"oi-gateway"}`))
	})

	log.Printf("Oi Real-Time WebSocket Relay running on :%s\n", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatalf("Server startup failed: %v", err)
	}
}
