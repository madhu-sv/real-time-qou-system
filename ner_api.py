from fastapi import FastAPI
from pydantic import BaseModel
import spacy
import json

app = FastAPI()
nlp = spacy.load("en_core_web_sm")

try:
    with open("patterns.json", "r") as f:
        patterns = json.load(f)
    ruler = nlp.add_pipe("entity_ruler", before="ner")
    ruler.add_patterns(patterns)
    print("--- Successfully loaded custom patterns from patterns.json ---")
except FileNotFoundError:
    print("--- WARNING: patterns.json not found. NER will only use the base ML model. ---")

class TextIn(BaseModel):
    text: str

@app.post("/ent")
async def extract_entities(payload: TextIn):
    # FIX: Convert the incoming text to lowercase before processing
    doc = nlp(payload.text.lower())
    entities = [{"text": ent.text, "label": ent.label_} for ent in doc.ents]
    return {"entities": entities}