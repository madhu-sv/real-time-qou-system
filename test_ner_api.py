from fastapi.testclient import TestClient
from ner_api import app # Import the FastAPI app object from your main script

# Create a TestClient instance
client = TestClient(app)

def test_extract_entities_with_known_brand():
    """
    Tests if the API correctly identifies a brand from our patterns.json.
    """
    # Arrange
    request_data = {"text": "I want a box of Kellogg's"}
    
    # Act
    response = client.post("/ent", json=request_data)
    
    # Assert
    assert response.status_code == 200
    response_json = response.json()
    assert "entities" in response_json
    
    entities = response_json["entities"]
    assert len(entities) > 0
    
    # Check if the 'kellogg's' brand was found
    found_brand = any(e['text'] == "kellogg's" and e['label'] == 'BRAND' for e in entities)
    assert found_brand, "The BRAND 'Kellogg\'s' was not found."

def test_extract_entities_with_no_known_entities():
    """
    Tests if the API returns an empty list for text with no entities.
    """
    # Arrange
    request_data = {"text": "a simple sentence"}
    
    # Act
    response = client.post("/ent", json=request_data)
    
    # Assert
    assert response.status_code == 200
    response_json = response.json()
    assert "entities" in response_json
    assert len(response_json["entities"]) == 0

def test_api_root_is_not_found():
    """
    A simple smoke test to ensure the server is running but the root path isn't defined.
    """
    response = client.get("/")
    assert response.status_code == 404 # Not Found, which is correct
    
def test_semantic_entity_recognition_with_md_model():
    """
    Tests if the larger 'md' model can identify general entities 
    (like a common brand and product type) that are not in our custom patterns file,
    showcasing its semantic understanding from word vectors.
    """
    # Arrange: This text contains entities not in our patterns.json
    request_data = {"text": "Can I buy some Adidas running shoes"}
    
    # Act
    response = client.post("/ent", json=request_data)
    
    # Assert
    assert response.status_code == 200
    response_json = response.json()
    assert "entities" in response_json
    
    entities = response_json["entities"]
    
    # Create a simple dictionary of the found entities for easier assertions
    # e.g., {"adidas": "BRAND", "running shoes": "PRODUCT"}
    found_entities = {e['text']: e['label'] for e in entities}
    
    # 1. Check if the model identified "adidas" as a BRAND
    assert "adidas" in found_entities
    assert found_entities["adidas"] == "BRAND"
    
    # 2. Check if the model identified "running shoes" as a PRODUCT
    assert "running shoes" in found_entities
    assert found_entities["running shoes"] == "PRODUCT"