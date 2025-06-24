# Use an official Python base image
FROM python:3.9-slim

LABEL authors="madhusudhanv"

# Set the working directory inside the container
WORKDIR /app

# 1. Install spaCy, FastAPI, and Uvicorn for the NER API
RUN pip install --no-cache-dir \
        spacy==3.7.2 \
        fastapi uvicorn

# 2. Download the specific English model we want to use
RUN python -m spacy download en_core_web_sm

# Expose the port the server will run on
EXPOSE 8000

# 3. Copy the custom NER API script and patterns file
COPY ner_api.py ./
COPY patterns.json ./

# 4. Run the FastAPI server on port 8000
CMD ["uvicorn", "ner_api:app", "--host", "0.0.0.0", "--port", "8000"]