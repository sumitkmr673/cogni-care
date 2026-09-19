import secrets
import string

ALPHANUMERIC_CHARS = string.ascii_uppercase + string.digits


def generate_public_id(prefix: str, length: int = 8) -> str:
    """Generate a human-facing public ID in the format PREFIX-XXXXXXXX."""
    token = "".join(secrets.choice(ALPHANUMERIC_CHARS) for _ in range(length))
    return f"{prefix}-{token}"


def generate_caregiver_public_id() -> str:
    return generate_public_id("CG", 8)


def generate_patient_public_id() -> str:
    return generate_public_id("PT", 8)


def generate_device_identifier() -> str:
    return generate_public_id("DEV", 12)
