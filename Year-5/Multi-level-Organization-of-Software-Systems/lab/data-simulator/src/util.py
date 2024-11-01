import datetime

def get_timestamp_now() -> float:
    return datetime.datetime.now(datetime.UTC).timestamp()
