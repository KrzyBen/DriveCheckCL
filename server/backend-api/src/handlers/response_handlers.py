from fastapi.responses import JSONResponse
from typing import Any


def handle_success(status_code: int, message: str, data: Any = {}) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "status": "Success",
            "message": message,
            "data": data,
        }
    )


def handle_error_client(status_code: int, message: str, details: Any = {}) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "status": "Client error",
            "message": message,
            "details": details,
        }
    )


def handle_error_server(status_code: int, message: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "status": "Server error",
            "message": message,
        }
    )
