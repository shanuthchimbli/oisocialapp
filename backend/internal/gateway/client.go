package gateway

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"chat.oi/backend/internal/model"
	"github.com/gorilla/websocket"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 65536 // 64 KB maximum frame size
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true // Configured via TLS 1.3 pinning in production
	},
}

type Client struct {
	Hub    *Hub
	Conn   *websocket.Conn
	Send   chan *model.ServerWsFrame
	send   chan *model.ServerWsFrame
	UserId string
}

func (c *Client) SendFrame(frame *model.ServerWsFrame) {
	select {
	case c.send <- frame:
	default:
		// Drop or handle slow consumer
	}
}

func (c *Client) readPump() {
	defer func() {
		c.Hub.unregister <- c
		c.Conn.Close()
	}()

	c.Conn.SetReadLimit(maxMessageSize)
	c.Conn.SetReadDeadline(time.Now().Add(pongWait))
	c.Conn.SetPongHandler(func(string) error {
		c.Conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := c.Conn.ReadMessage()
		if err != nil {
			break
		}

		var frame model.ClientWsFrame
		if err := json.Unmarshal(message, &frame); err != nil {
			continue
		}

		switch frame.Type {
		case "AUTH":
			// Token validation; for baseline dev, parse token directly
			c.UserId = frame.Token
			c.Hub.register <- c

		case "MSG_SUBMIT":
			c.Hub.RouteMessage(c.UserId, &frame)

		case "MSG_DELIVERED_ACK":
			c.Hub.RouteReceipt("MSG_DELIVERED_RECEIPT", frame.MsgId, frame.RecipientId)

		case "MSG_READ_ACK":
			c.Hub.RouteReceipt("MSG_READ_RECEIPT", frame.MsgId, frame.RecipientId)

		case "PING":
			c.SendFrame(&model.ServerWsFrame{Type: "PONG"})
		}
	}
}

func (c *Client) writePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.Conn.Close()
	}()

	for {
		select {
		case frame, ok := <-c.send:
			c.Conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			payload, err := json.Marshal(frame)
			if err != nil {
				continue
			}

			if err := c.Conn.WriteMessage(websocket.TextMessage, payload); err != nil {
				return
			}

		case <-ticker.C:
			c.Conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func ServeWs(hub *Hub, w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("WebSocket upgrade error: %v", err)
		return
	}

	sendChan := make(chan *model.ServerWsFrame, 256)
	client := &Client{
		Hub:  hub,
		Conn: conn,
		Send: sendChan,
		send: sendChan,
	}

	go client.writePump()
	go client.readPump()
}
