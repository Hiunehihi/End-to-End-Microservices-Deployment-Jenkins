import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProductList from './components/ProductList';
import Cart from './components/Cart';
import Checkout from './components/Checkout';
import PaymentResult from './components/PaymentResult';
import Login from './components/Login';
import Register from './components/Register';
import MyOrders from './components/MyOrders';
import Profile from './components/Profile';
import ProtectedRoute from './components/ProtectedRoute';
import AdminLayout from './components/admin/AdminLayout';
import Dashboard from './components/admin/Dashboard';
import ProductManagement from './components/admin/ProductManagement';
import OrderManagement from './components/admin/OrderManagement';
import InventoryManagement from './components/admin/InventoryManagement';
import UserManagement from './components/admin/UserManagement';
import { CartProvider } from './context/CartContext';
import { AuthProvider } from './context/AuthContext';
import './index.css';

const App: React.FC = () => {
    return (
        <AuthProvider>
            <CartProvider>
                <Router>
                    <Routes>
                        <Route
                            path="/admin"
                            element={
                                <ProtectedRoute role="ADMIN">
                                    <AdminLayout />
                                </ProtectedRoute>
                            }
                        >
                            <Route index element={<Dashboard />} />
                            <Route path="products" element={<ProductManagement />} />
                            <Route path="orders" element={<OrderManagement />} />
                            <Route path="inventory" element={<InventoryManagement />} />
                            <Route path="users" element={<UserManagement />} />
                        </Route>

                        <Route
                            path="/*"
                            element={
                                <div className="app">
                                    <Navbar />
                                    <main className="container">
                                        <Routes>
                                            <Route path="/" element={<ProductList />} />
                                            <Route path="/login" element={<Login />} />
                                            <Route path="/register" element={<Register />} />
                                            <Route
                                                path="/cart"
                                                element={<ProtectedRoute><Cart /></ProtectedRoute>}
                                            />
                                            <Route
                                                path="/checkout"
                                                element={<ProtectedRoute><Checkout /></ProtectedRoute>}
                                            />
                                            <Route path="/payment-result" element={<PaymentResult />} />
                                            <Route
                                                path="/my-orders"
                                                element={<ProtectedRoute><MyOrders /></ProtectedRoute>}
                                            />
                                            <Route
                                                path="/profile"
                                                element={<ProtectedRoute><Profile /></ProtectedRoute>}
                                            />
                                        </Routes>
                                    </main>
                                </div>
                            }
                        />
                    </Routes>
                </Router>
            </CartProvider>
        </AuthProvider>
    );
};

export default App;
