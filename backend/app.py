# app.py
from flask import Flask, request, jsonify
import google.generativeai as genai
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Configure API key
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

# Initialize Flask app
app = Flask(__name__)


@app.route('/summarize', methods=['POST'])
def summarize_meeting():
    # Get raw meeting notes from request
    data = request.json
    if not data or 'meetingNotes' not in data:
        return jsonify({"error": "No meeting notes provided"}), 400

    raw_notes = data['meetingNotes']

    try:
        # Configure the model
        model = genai.GenerativeModel('gemini-2.0-flash')

        # Create prompt for summarization
        prompt = """
        Please summarize the following meeting notes into a structured, concise format.
        Include key decisions, action items, and important discussion points:

        {}
        """.format(raw_notes)

        # Generate response from Gemini
        response = model.generate_content(prompt)

        # Return the summarized text
        return jsonify({
            "success": True,
            "summary": response.text
        })

    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


if __name__ == '__main__':
    app.run(debug=True, port=5000)