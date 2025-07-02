# Use an official Python base image
FROM python:3.9-slim

LABEL authors="madhusudhanv"

# First, update the package list, then install the 'build-essential' package
RUN apt-get update && apt-get install -y build-essential

# Set the working directory inside the container
WORKDIR /app

COPY requirements.txt /app/
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# Download the medium English model
RUN python -m spacy download en_core_web_md

# Copy application code, custom patterns, and fine-tuned model
COPY ner_api.py patterns.json fine_tuned_ner_model /app/

# Expose the port the server will run on
EXPOSE 8000

# Start the FastAPI server using uvicorn
CMD ["uvicorn", "ner_api:app", "--host", "0.0.0.0", "--port", "8000"]