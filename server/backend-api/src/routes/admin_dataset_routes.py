from fastapi import APIRouter, Depends
from middlewares.authentication_middleware import authenticate_jwt
from middlewares.authorization_middleware import is_admin
from controllers.dataset_controller import (
    obtener_resumen_dataset, listar_clips_dataset, actualizar_clip_dataset,
    restablecer_clip_dataset, exportar_dataset_admin, descargar_dataset,
)

router = APIRouter(dependencies=[Depends(authenticate_jwt), Depends(is_admin)])

router.add_api_route("/",                           obtener_resumen_dataset,  methods=["GET"])
router.add_api_route("/clips",                      listar_clips_dataset,     methods=["GET"])
router.add_api_route("/clips/{video_id}",           actualizar_clip_dataset,  methods=["PATCH"])
router.add_api_route("/clips/{video_id}/etiquetas", restablecer_clip_dataset, methods=["DELETE"])
router.add_api_route("/exportar",                   exportar_dataset_admin,   methods=["POST"])
router.add_api_route("/descargar",                  descargar_dataset,        methods=["GET"])
