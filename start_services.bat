@echo off
start "backend" cmd /c "cd /d E:\xmj\java\examinationEn\backend && mvn spring-boot:run"
start "frontend" cmd /c "cd /d E:\xmj\java\examinationEn\frontend && npm run dev"
start "chroma" powershell -NoExit -Command "Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; & 'E:\xmj\java\examinationEn\.venv\Scripts\Activate.ps1'; chroma run --host 0.0.0.0 --port 8000"
start "python-services" powershell -NoExit -Command "Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; & 'E:\xmj\java\examinationEn\.venv\Scripts\Activate.ps1'; cd E:\xmj\java\examinationEn\python-services; python run.py"
start "chromadb-admin" powershell -NoExit -Command "Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; & 'E:\xmj\java\examinationEn\.venv\Scripts\Activate.ps1'; python E:\xmj\java\knowledgeManager\chromadb_web_admin.py"
echo started
