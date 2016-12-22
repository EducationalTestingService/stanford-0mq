import sys
import json

import zmq


broker_addr = sys.argv[1]

context = zmq.Context()

print("Connecting to server...", file=sys.stderr)
socket = context.socket(zmq.REQ)
socket.connect(broker_addr)
print(file=sys.stderr)

request = json.dumps({"tokens" : [["Hello", ",", "world", "!"], ["How", "are", "you", "?"]]})
print("Sending request", request, file=sys.stderr)
socket.send_string(request, 0)
print(file=sys.stderr)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["tags"])
print()

request = json.dumps(result)
print("Sending request", result, file=sys.stderr)
socket.send_string(request, 0)
print(file=sys.stderr)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["trees"])
print()

request = json.dumps({"text" : "Hello, world!  Please tokenize this text.  Thanks a lot!"})
print("Sending request", request, file=sys.stderr)
socket.send_string(request, 0)
print(file=sys.stderr)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["tokens"])
print()

socket.close()
context.term()
