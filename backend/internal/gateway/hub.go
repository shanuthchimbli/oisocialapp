package gateway

import (
	"sync"
	"time"

	"chat.oi/backend/internal/model"
)

type Hub struct {
	sync.RWMutex
	clients    map[string]*Client // maps userId -> active Client (Single Active Device)
	register   chan *Client
	unregister chan *Client
	broadcast  chan *model.ServerWsFrame
}

func NewHub() *Hub {
	return &Hub{
		clients:    make(map[string]*Client),
		register:   make(chan *Client),
		unregister: make(chan *Client),
		broadcast:  make(chan *model.ServerWsFrame),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.Lock()
			// Enforce Single Active Device Invariant (PRD-005, FUNC-DEV-001)
			if existing, ok := h.clients[client.UserId]; ok && existing != client {
				// Evict and terminate existing session immediately
				existing.SendFrame(&model.ServerWsFrame{
					Type:   "SESSION_TERMINATED",
					Reason: "LOGGED_IN_ON_ANOTHER_DEVICE",
				})
				close(existing.send)
				delete(h.clients, client.UserId)
			}
			h.clients[client.UserId] = client
			h.Unlock()

			client.SendFrame(&model.ServerWsFrame{
				Type:   "AUTH_ACK",
				Status: "AUTHENTICATED",
			})

		case client := <-h.unregister:
			h.Lock()
			if current, ok := h.clients[client.UserId]; ok && current == client {
				delete(h.clients, client.UserId)
				close(client.send)
			}
			h.Unlock()
		}
	}
}

func (h *Hub) RouteMessage(senderId string, frame *model.ClientWsFrame) {
	now := time.Now().UnixMilli()

	// 1. Immediately acknowledge sender (Single checkmark: SENT)
	h.RLock()
	sender, senderOnline := h.clients[senderId]
	recipient, recipientOnline := h.clients[frame.RecipientId]
	h.RUnlock()

	if senderOnline {
		sender.SendFrame(&model.ServerWsFrame{
			Type:      "MSG_ACK",
			MsgId:     frame.MsgId,
			ServerSeq: now,
			Timestamp: now,
		})
	}

	// 2. Blind Mailbox Relay to Recipient
	if recipientOnline {
		recipient.SendFrame(&model.ServerWsFrame{
			Type:      "MSG_DELIVER",
			MsgId:     frame.MsgId,
			SenderId:  senderId,
			Envelope:  frame.Envelope,
			ServerSeq: now,
			Timestamp: now,
		})
	} else {
		// In production, persist to transit_messages table for delayed delivery
	}
}

func (h *Hub) RouteReceipt(receiptType string, msgId string, targetUserId string) {
	h.RLock()
	target, online := h.clients[targetUserId]
	h.RUnlock()

	if online {
		target.SendFrame(&model.ServerWsFrame{
			Type:      receiptType,
			MsgId:     msgId,
			Timestamp: time.Now().UnixMilli(),
		})
	}
}
