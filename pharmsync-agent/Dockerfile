FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1
ENV PORT=8080

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt uvicorn fastapi google-adk

COPY app/ ./app/
COPY ui/ ./ui/

EXPOSE 8080

CMD ["python", "ui/server.py"]
