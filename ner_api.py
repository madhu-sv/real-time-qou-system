from fastapi import FastAPI
from pydantic import BaseModel
import spacy
import json
import logging

# --- Setup basic logging ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI()

# Load the fine-tuned model if available, otherwise fall back to the medium MD model
try:
    nlp = spacy.load("fine_tuned_ner_model")
    logger.info("Loaded fine-tuned NER model from 'fine_tuned_ner_model'")
except OSError:
    nlp = spacy.load("en_core_web_md")
    logger.info("Loaded spaCy 'en_core_web_md' model")

try:
    with open("patterns.json", "r") as f:
        patterns = json.load(f)
    ruler = nlp.add_pipe("entity_ruler", after="ner", config={"overwrite_ents": True})
    ruler.add_patterns(patterns)
    logger.info("Successfully loaded custom patterns from patterns.json")
except FileNotFoundError:
    logger.warning("patterns.json not found. NER will only use the base ML model.")
except Exception as e:
    logger.error(f"Could not load patterns from patterns.json: {e}")

class TextIn(BaseModel):
    text: str

@app.post("/ent")
async def extract_entities(payload: TextIn):
    """
    Takes a raw text string and returns a list of named entities
    found by the spaCy pipeline (custom EntityRuler + ML model).
    """
    doc = nlp(payload.text.lower())
    entities = [{"text": ent.text, "label": ent.label_} for ent in doc.ents]
    return {"entities": entities}