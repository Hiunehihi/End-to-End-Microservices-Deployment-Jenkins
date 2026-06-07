import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface Props {
    children: React.ReactElement;
    role?: string;
}

const ProtectedRoute: React.FC<Props> = ({ children, role }) => {
    const { isAuthenticated, hasRole } = useAuth();
    const location = useLocation();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }
    if (role && !hasRole(role)) {
        return <Navigate to="/" replace />;
    }
    return children;
};

export default ProtectedRoute;
