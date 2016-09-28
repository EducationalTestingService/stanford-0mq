import sys
import json

import zmq


broker_addr = "tcp://127.0.0.1:5555"

context = zmq.Context()

print("Connecting to server...", file=sys.stderr)
socket = context.socket(zmq.REQ)
socket.connect(broker_addr)

request = json.dumps({"tokens" : [["Hello", ",", "world", "!"], ["How", "are", "you", "?"]]})
print("Sending request", request, file=sys.stderr)
socket.send_string(request, 0)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["tags"])

request = json.dumps(result)
print("Sending request", result, file=sys.stderr)
socket.send_string(request, 0)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["trees"])

socket.close()
context.term()
