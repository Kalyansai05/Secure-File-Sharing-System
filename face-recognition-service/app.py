# app.py (Modified)

from flask import Flask, request, jsonify
from flask_cors import CORS
import os, base64, io
from PIL import Image
import numpy as np
import face_recognition
from werkzeug.utils import secure_filename # Not used in verification, but harmless

app = Flask(__name__)
# Allow CORS from your Spring Boot app's origin (e.g., http://localhost:8080)
CORS(app)

TOLERANCE = 0.7
MODEL = "hog"

# REMOVED: load_known_faces() and known_encodings/known_names
# since we are doing 1:1 comparison now.

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status":"ok"})

# 🛑 MODIFIED ENDPOINT TO MATCH JAVA SERVICE CALL
@app.route("/api/face-verify", methods=["POST"])
def verify_face():
    data = request.get_json(silent=True) or {}

    # --- 1. Get and Decode Live Image ---
    image_b64 = data.get("image_base64")
    template_b64 = data.get("template_base64") # 🛑 NEW: Get template from Java

    if not image_b64 or not template_b64:
        return jsonify({"status":"error", "message":"Missing image or template"}), 400

    if image_b64.startswith("data:"):
        image_b64 = image_b64.split(",", 1)[1]

    try:
        live_img = np.array(Image.open(io.BytesIO(base64.b64decode(image_b64))).convert("RGB"))
        template_img = np.array(Image.open(io.BytesIO(base64.b64decode(template_b64))).convert("RGB"))
    except Exception:
        return jsonify({"status":"error", "message":"Image decoding failed"}), 400

    # --- 2. Encode Template Image (The Known Face) ---
    template_encs = face_recognition.face_encodings(template_img)
    if len(template_encs) == 0:
        return jsonify({"status":"failed", "reason":"template_no_face"})
    template_encoding = template_encs[0]

    # --- 3. Encode Live Image (The Unknown Face) ---
    face_locations = face_recognition.face_locations(live_img, model=MODEL)
    face_encodings = face_recognition.face_encodings(live_img, face_locations)

    if len(face_encodings) == 0:
        return jsonify({"status":"failed", "reason":"no_face_detected"})

    # --- 4. Compare (1:1 Verification) ---
    for enc in face_encodings:
        distances = face_recognition.face_distance([template_encoding], enc)

        if distances[0] <= TOLERANCE:
            return jsonify({"status":"success"})

    return jsonify({"status":"failed", "reason":"no_match"})

# The /add-known-face route can remain if you need it for registration or setup
# ... (rest of the Flask code) ...

if __name__ == "__main__":
    # print("Loading known faces...") # REMOVED: No longer loading static faces
    app.run(host="0.0.0.0", port=5000)