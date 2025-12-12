// src/components/Auth/LoginForm.jsx
import React, { useState } from 'react';
import { TextField, Button, Box, Typography } from '@mui/material';
import { api } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

export default function LoginForm() {
    const [formData, setFormData] = useState({ username: '', password: '' });
    const [error, setError] = useState('');
    const { login } = useAuth(); // Get login function from context

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const response = await api.login(formData);
            // Assume response is { token, userId, username, displayName, roles, avatarUrl }
            login(response.token, { // Pass token and user data to context
                userId: response.userId,
                username: response.username,
                displayName: response.displayName,
                roles: response.roles,
                avatarUrl: response.avatarUrl,
            });
            // Optionally navigate away after successful login
            // window.location.href = '/feed'; // Or use react-router-dom's navigate hook
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <TextField
                fullWidth
                margin="normal"
                name="username"
                label="Username"
                value={formData.username}
                onChange={handleChange}
                required
            />
            <TextField
                fullWidth
                margin="normal"
                name="password"
                label="Password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                required
            />
            {error && <Typography color="error" variant="caption">{error}</Typography>}
            <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }}>
                Login
            </Button>
        </form>
    );
}