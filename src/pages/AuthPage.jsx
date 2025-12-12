// src/pages/AuthPage.jsx
import React, { useState } from 'react';
import { Container, Paper, Tabs, Tab, Box, Typography } from '@mui/material';
import LoginForm from '../components/Auth/LoginForm';
import RegisterForm from '../components/Auth/RegisterForm';

export default function AuthPage() {
    const [activeTab, setActiveTab] = useState(0);

    const handleChangeTab = (event, newValue) => {
        setActiveTab(newValue);
    };

    return (
        <Container maxWidth="sm" sx={{ marginTop: 4 }}>
            <Paper elevation={3} sx={{ padding: 3 }}>
                <Tabs value={activeTab} onChange={handleChangeTab} centered>
                    <Tab label="Login" />
                    <Tab label="Register" />
                </Tabs>
                <Box sx={{ paddingTop: 2 }}>
                    {activeTab === 0 && <LoginForm />}
                    {activeTab === 1 && <RegisterForm />}
                </Box>
            </Paper>
        </Container>
    );
}