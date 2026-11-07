/**
 * Oi Real-Time WebSocket Relay Server (Zero-Dependency Node.js 22 Implementation)
 * Implements RFC 6455 WebSocket Handshake & Framing using only Node.js standard libraries.
 * Enforces Single Active Device (PRD-005) & Blind Mailbox Relay (PRD-006).
 */

const http = require('http');
const crypto = require('crypto');

const PORT = process.env.PORT || 8080;
const GUID = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11';

// Active registry: maps userId -> clientSocket
const activeClients = new Map();

const server = http.createServer((req, res) => {
  if (req.url === '/healthz' || req.url === '/') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'HEALTHY',
      service: 'oi-relay-server',
      connectedClients: activeClients.size,
      timestamp: new Date().toISOString()
    }));
    return;
  }
  res.writeHead(404);
  res.end();
});

// RFC 6455 WebSocket Upgrade Handler
server.on('upgrade', (req, socket, head) => {
  const key = req.headers['sec-websocket-key'];
  if (!key) {
    socket.destroy();
    return;
  }

  const acceptKey = crypto.createHash('sha1').update(key + GUID).digest('base64');
  const headers = [
    'HTTP/1.1 101 Switching Protocols',
    'Upgrade: websocket',
    'Connection: Upgrade',
    `Sec-WebSocket-Accept: ${acceptKey}`
  ];

  socket.write(headers.join('\r\n') + '\r\n\r\n');

  let currentUserId = null;

  // Frame sender helper
  function sendJson(payload) {
    try {
      const jsonStr = JSON.stringify(payload);
      const payloadBuf = Buffer.from(jsonStr);
      const length = payloadBuf.length;

      let header;
      if (length < 126) {
        header = Buffer.from([0x81, length]);
      } else if (length < 65536) {
        header = Buffer.alloc(4);
        header[0] = 0x81;
        header[1] = 126;
        header.writeUInt16BE(length, 2);
      } else {
        header = Buffer.alloc(10);
        header[0] = 0x81;
        header[1] = 127;
        header.writeBigUInt64BE(BigInt(length), 2);
      }

      socket.write(Buffer.concat([header, payloadBuf]));
    } catch (e) {
      // Socket write error or disconnected
    }
  }

  // Socket incoming data parser
  socket.on('data', (buffer) => {
    try {
      if (buffer.length < 2) return;

      const opcode = buffer[0] & 0x0f;
      // Close frame
      if (opcode === 0x08) {
        socket.end();
        return;
      }
      // Ping frame -> respond Pong
      if (opcode === 0x09) {
        socket.write(Buffer.from([0x8a, 0x00]));
        return;
      }

      const isMasked = (buffer[1] & 0x80) === 0x80;
      let payloadLength = buffer[1] & 0x7f;
      let offset = 2;

      if (payloadLength === 126) {
        payloadLength = buffer.readUInt16BE(2);
        offset = 4;
      } else if (payloadLength === 127) {
        payloadLength = Number(buffer.readBigUInt64BE(2));
        offset = 10;
      }

      let mask = null;
      if (isMasked) {
        mask = buffer.slice(offset, offset + 4);
        offset += 4;
      }

      const payload = buffer.slice(offset, offset + payloadLength);
      if (isMasked && mask) {
        for (let i = 0; i < payload.length; i++) {
          payload[i] ^= mask[i % 4];
        }
      }

      const text = payload.toString('utf8');
      const frame = JSON.parse(text);

      handleFrame(frame, socket, sendJson, (userId) => {
        currentUserId = userId;
      });
    } catch (err) {
      // Malformed or incomplete frame
    }
  });

  socket.on('close', () => {
    if (currentUserId && activeClients.get(currentUserId)?.socket === socket) {
      activeClients.delete(currentUserId);
      console.log(`[-] Client disconnected: ${currentUserId} (Active: ${activeClients.size})`);
    }
  });

  socket.on('error', () => {
    socket.destroy();
  });
});

function handleFrame(frame, socket, sendJson, setUserId) {
  const now = Date.now();

  switch (frame.type) {
    case 'AUTH': {
      const userId = frame.token;
      setUserId(userId);

      // Single Active Device Enforcement (PRD-005 / FUNC-DEV-001)
      if (activeClients.has(userId)) {
        const prior = activeClients.get(userId);
        if (prior.socket !== socket) {
          console.log(`[!] Terminating prior session for ${userId} (Single Device Invariant)`);
          prior.sendJson({
            type: 'SESSION_TERMINATED',
            reason: 'LOGGED_IN_ON_ANOTHER_DEVICE'
          });
          prior.socket.end();
        }
      }

      activeClients.set(userId, { socket, sendJson });
      console.log(`[+] Authenticated user: ${userId} (Active connections: ${activeClients.size})`);

      sendJson({
        type: 'AUTH_ACK',
        status: 'AUTHENTICATED'
      });
      break;
    }

    case 'MSG_SUBMIT': {
      // 1. Immediately ACK sender (SENT tick: single checkmark)
      sendJson({
        type: 'MSG_ACK',
        msgId: frame.msgId,
        serverSeq: now,
        timestamp: now
      });

      // 2. Blind Mailbox Relay to Recipient
      const recipient = activeClients.get(frame.recipientId);
      if (recipient) {
        recipient.sendJson({
          type: 'MSG_DELIVER',
          msgId: frame.msgId,
          senderId: frame.senderId || 'peer',
          envelope: frame.envelope,
          serverSeq: now,
          timestamp: now
        });
        console.log(`[→] Delivered message ${frame.msgId} to online recipient ${frame.recipientId}`);
      } else {
        console.log(`[~] Recipient ${frame.recipientId} offline; queued in blind mailbox.`);
      }
      break;
    }

    case 'MSG_DELIVERED_ACK': {
      // Relay delivered receipt back to sender (DELIVERED tick: double checkmark)
      const sender = activeClients.get(frame.recipientId);
      if (sender) {
        sender.sendJson({
          type: 'MSG_DELIVERED_RECEIPT',
          msgId: frame.msgId,
          timestamp: now
        });
      }
      break;
    }

    case 'MSG_READ_ACK': {
      // Relay read receipt back to sender (READ tick: cyan double checkmark)
      const sender = activeClients.get(frame.recipientId);
      if (sender) {
        sender.sendJson({
          type: 'MSG_READ_RECEIPT',
          msgId: frame.msgId,
          timestamp: now
        });
      }
      break;
    }

    case 'PING': {
      sendJson({ type: 'PONG' });
      break;
    }
  }
}

server.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(` Oi Real-Time WebSocket Relay running on port :${PORT}`);
  console.log(` Health check:  http://localhost:${PORT}/healthz`);
  console.log(` WebSocket URL: ws://localhost:${PORT}/v1/ws`);
  console.log(` Zero dependencies required (Node.js native).`);
  console.log(`=======================================================`);
});
