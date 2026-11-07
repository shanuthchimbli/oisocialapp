package model

import "encoding/json"

// ClientWsFrame represents frames incoming from mobile clients
type ClientWsFrame struct {
	Type            string          `json:"type"` // AUTH, MSG_SUBMIT, MSG_DELIVERED_ACK, MSG_READ_ACK, PING
	Token           string          `json:"token,omitempty"`
	MsgId           string          `json:"msgId,omitempty"`
	RecipientId     string          `json:"recipientId,omitempty"`
	Envelope        json.RawMessage `json:"envelope,omitempty"`
	ClientTimestamp int64           `json:"clientTimestamp,omitempty"`
}

// ServerWsFrame represents frames outgoing to mobile clients
type ServerWsFrame struct {
	Type        string          `json:"type"` // AUTH_ACK, MSG_ACK, MSG_DELIVER, MSG_DELIVERED_RECEIPT, MSG_READ_RECEIPT, SESSION_TERMINATED, PONG
	Status      string          `json:"status,omitempty"`
	MsgId       string          `json:"msgId,omitempty"`
	SenderId    string          `json:"senderId,omitempty"`
	Envelope    json.RawMessage `json:"envelope,omitempty"`
	ServerSeq   int64           `json:"serverSeq,omitempty"`
	Timestamp   int64           `json:"timestamp,omitempty"`
	Reason      string          `json:"reason,omitempty"`
}
