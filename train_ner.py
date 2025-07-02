import spacy
import random
from spacy.training.example import Example
import logging

# --- Setup basic logging ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 1. Our "Golden" Labeled Data
TRAIN_DATA = [
    ("I am looking for Fage yogurt", {"entities": [(17, 21, "BRAND"), (22, 28, "PRODUCT_TYPE")]}),
    ("show me some organic avocados", {"entities": [(13, 20, "DIETARY_ATTRIBUTE"), (21, 29, "PRODUCT_TYPE")]}),
    ("Do you have Bonne Maman jam?", {"entities": [(12, 23, "BRAND"), (24, 27, "PRODUCT_TYPE")]}),
    ("I need Kellogg's corn flakes", {"entities": [(8, 17, "BRAND"), (18, 29, "PRODUCT_TYPE")]}),
    ("find some gluten-free bread", {"entities": [(10, 21, "DIETARY_ATTRIBUTE"), (22, 27, "PRODUCT_TYPE")]}),
    ("where is the Horizon Organic milk", {"entities": [(13, 28, "BRAND"), (29, 33, "PRODUCT_TYPE")]})
]

def train_spacy_ner_model(data, iterations=20):
    """Loads a pre-trained model and fine-tunes it with new entity examples."""

    # Load the medium pre-trained spaCy model
    logger.info("Loading pre-trained model 'en_core_web_md'...")
    nlp = spacy.load("en_core_web_md")
    ner = nlp.get_pipe("ner")
    
    for _, annotations in data:
        for ent in annotations.get("entities"):
            ner.add_label(ent[2])
            
    pipe_exceptions = ["ner", "trf_wordpiecer", "trf_tok2vec"]
    unaffected_pipes = [pipe for pipe in nlp.pipe_names if pipe not in pipe_exceptions]
    
    logger.info("Starting training loop...")
    with nlp.select_pipes(disable=unaffected_pipes):
        optimizer = nlp.begin_training()
        for iteration in range(iterations):
            random.shuffle(data)
            losses = {}
            for text, annotations in data:
                doc = nlp.make_doc(text)
                example = Example.from_dict(doc, annotations)
                nlp.update([example], drop=0.5, sgd=optimizer, losses=losses)
            logger.info(f"Iteration {iteration + 1}/{iterations} - Losses: {losses}")

    return nlp

if __name__ == "__main__":
    fine_tuned_nlp = train_spacy_ner_model(TRAIN_DATA)
    output_dir = "./fine_tuned_ner_model"
    fine_tuned_nlp.to_disk(output_dir)
    logger.info(f"Training complete. Model saved to '{output_dir}'")
    
    logger.info("--- Testing the fine-tuned model ---")
    test_text = "I need some Fage"
    doc = fine_tuned_nlp(test_text)
    logger.info(f"Entities in '{test_text}':")
    for ent in doc.ents:
        logger.info(f"  - '{ent.text}' ({ent.label_})")