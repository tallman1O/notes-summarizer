# app.py
from flask import Flask, request, jsonify
import google.generativeai as genai
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Configure API key
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

app = Flask(__name__)

@app.route('/generate_notes', methods=['POST'])
def generate_notes():
    data = request.json

    if not data or 'lectureNotes' not in data:
        return jsonify({"error": "No lecture notes provided"}), 400

    notes = data['lectureNotes']
    subject = data.get('subject', 'General')

    try:
        model = genai.GenerativeModel('gemini-2.0-flash')

        prompt = f"""
        You are an AI assistant helping students generate concise and clear study notes.

        Subject: {subject}

        Based on the following lecture notes, do two things:
        1. Generate well-structured and summarized study notes.
        2. Create 3 to 5 relevant quiz questions that help test understanding.

        Lecture Notes:
        {notes}
        """

        response = model.generate_content(prompt)

        if hasattr(response, 'text'):
            # Attempt to split the notes and quiz questions
            if "Quiz Questions:" in response.text:
                parts = response.text.split("Quiz Questions:")
                study_notes = parts[0].strip()
                quiz = parts[1].strip()
            else:
                study_notes = response.text.strip()
                quiz = "No quiz questions found."

            return jsonify({
                "success": True,
                "notes": study_notes,
                "quiz": quiz
            })

        else:
            return jsonify({"success": False, "error": "No text returned from model."})

    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

if __name__ == '__main__':
    app.run(debug=True, port=5000)
