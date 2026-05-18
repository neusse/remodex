use std::sync::Arc;

use axum::{extract::State, Json};
use serde_json::{json, Value};

use crate::provider_bridge::server::ProviderBridgeAppState;

pub async fn get_models(State(state): State<Arc<ProviderBridgeAppState>>) -> Json<Value> {
    Json(json!({
        "object": "list",
        "data": [
            {
                "id": state.config.default_model.clone(),
                "object": "model",
                "owned_by": state.config.provider.clone()
            },
            {
                "id": "deepseek-v4-flash",
                "object": "model",
                "owned_by": state.config.provider.clone()
            }
        ]
    }))
}
