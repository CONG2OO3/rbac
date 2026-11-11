import request from "./request";

export function listProducts(params) {
  return request.get("/business/products", { params });
}

export function productOptions() {
  return request.get("/business/products/options");
}

export function createProduct(data) {
  return request.post("/business/products", data);
}

export function updateProduct(id, data) {
  return request.put(`/business/products/${id}`, data);
}

export function deleteProduct(id) {
  return request.delete(`/business/products/${id}`);
}

export function listOrders(params) {
  return request.get("/business/orders", { params });
}

export function getOrder(id) {
  return request.get(`/business/orders/${id}`);
}

export function createOrder(data) {
  return request.post("/business/orders", data);
}

export function updateOrder(id, data) {
  return request.put(`/business/orders/${id}`, data);
}

export function changeOrderStatus(id, status) {
  return request.put(`/business/orders/${id}/status`, { status });
}

export function deleteOrder(id) {
  return request.delete(`/business/orders/${id}`);
}
