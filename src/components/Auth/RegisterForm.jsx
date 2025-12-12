// src/components/Auth/RegisterForm.jsx
import React, { useState } from 'react';
import { TextField, Button, Box, Typography } from '@mui/material';
import { api } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

export default function RegisterForm() {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        displayName: '',
        password: ''
    });
    const [error, setError] = useState('');
    const { login } = useAuth(); // Get login function from context (to log in immediately after registration)

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const response = await api.register(formData);
            // Assume response is { token, userId, username, displayName, roles, avatarUrl }
            login(response.token, { // Log in the user immediately
                userId: response.userId,
                username: response.username,
                displayName: response.displayName,
                roles: response.roles,
                avatarUrl: response.avatarUrl,
            });
            // Optionally navigate away after successful registration
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
                name="email"
                label="Email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                required
            />
            <TextField
                fullWidth
                margin="normal"
                name="displayName"
                label="Display Name"
                value={formData.displayName}
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
            <Button type="submit" variant="contained" color="secondary" fullWidth sx={{ mt: 2 }}>
                Register
            </Button>
        </form>
    );
}