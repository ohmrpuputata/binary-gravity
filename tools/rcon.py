"""Minimal Source RCON client. Runs each argv command against the local dev server.

Usage:  python tools/rcon.py "time set day" "summon ..." ...
"""
import socket
import struct
import sys

HOST, PORT, PW = "127.0.0.1", 25575, "test123"


def recvn(sock, n):
    buf = b""
    while len(buf) < n:
        c = sock.recv(n - len(buf))
        if not c:
            raise IOError("connection closed")
        buf += c
    return buf


def send(sock, req_id, typ, body):
    data = struct.pack("<ii", req_id, typ) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(data)) + data)


def recv(sock):
    ln = struct.unpack("<i", recvn(sock, 4))[0]
    payload = recvn(sock, ln)
    req_id, typ = struct.unpack("<ii", payload[:8])
    return req_id, typ, payload[8:-2].decode("utf-8", "replace")


def main():
    cmds = sys.argv[1:]
    s = socket.create_connection((HOST, PORT), timeout=10)
    send(s, 1, 3, PW)
    rid, _, _ = recv(s)
    if rid == -1:
        print("RCON AUTH FAILED")
        sys.exit(1)
    for c in cmds:
        send(s, 2, 2, c)
        _, _, body = recv(s)
        print(f"> {c}\n  {body.strip()}")
    s.close()


if __name__ == "__main__":
    main()
