import React, { useState } from 'react';
import { apiBase } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { Button, Paper, Stack, TextField, Typography } from "@mui/material";

export default function AuthPage({ active, onPage }) {
    const { setAuth } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });

    const [login, setLogin] = useState({ username: '', password: '' });
    const [reg, setReg] = useState({ username: '', email: '', displayName: '', password: '' });

    const [isLogin, setIsLogin] = useState(true); // Статус текущей вкладки (Вход / Регистрация)

    function setStatusAuth(msg = '', type = '') {
        setStatus({ msg, type });
    }

    async function doLogin() {
        const username = login.username.trim();
        const password = login.password;

        if (!username || !password) {
            setStatusAuth('Введите логин и пароль', 'error');
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });

            if (res.status === 401 || res.status === 403) {
                setStatusAuth('Неверный логин или пароль', 'error');
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const data = await res.json();
            const nextUser = {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: Array.isArray(data.roles) ? data.roles : [],
                avatarUrl: data.avatarUrl ?? null,
            };
            setAuth(data.token, nextUser);
            setStatusAuth('Успешный вход', 'ok');

            setLogin({ username: '', password: '' });
            onPage("account"); // Переключаем после успешного входа
        } catch (e) {
            setStatusAuth('Ошибка входа: ' + e.message, 'error');
        }
    }

    async function doRegister() {
        const username = reg.username.trim();
        const email = reg.email.trim();
        const displayName = reg.displayName.trim();
        const password = reg.password;

        if (!username || !email || !displayName || !password) {
            setStatusAuth('Заполните все поля регистрации', 'error');
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, displayName, password }),
            });

            if (res.status === 409) {
                setStatusAuth('Такой логин или email уже существует', 'error');
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const data = await res.json();
            const nextUser = {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: Array.isArray(data.roles) ? data.roles : [],
                avatarUrl: data.avatarUrl ?? null,
            };
            setAuth(data.token, nextUser);
            setStatusAuth('Регистрация прошла успешно', 'ok');

            setReg({ username: '', email: '', displayName: '', password: '' });
            onPage("account"); // Переключаем после успешной регистрации
        } catch (e) {
            setStatusAuth('Ошибка регистрации: ' + e.message, 'error');
        }
    }

    const toggleForm = () => {
        setIsLogin(!isLogin); // Переключаем форму
    };

    if (!active) return null;

    const loginDisabled =
        !login.username.trim() ||
        !login.password;

    const regDisabled =
        !reg.username.trim() ||
        !reg.email.trim() ||
        !reg.displayName.trim() ||
        !reg.password;

    return (
        <Paper elevation={0} sx={{ flex: 1 }}>
            <Stack p={2} spacing={2} height={1}>
                {isLogin ? (
                    <>
                        <Typography variant="h6">Вход</Typography>
                        <TextField
                            size="small"
                            placeholder="Логин"
                            value={login.username}
                            onChange={(e) =>
                                setLogin((s) => ({ ...s, username: e.target.value }))
                            }
                            autoFocus
                        />
                        <TextField
                            size="small"
                            type="password"
                            placeholder="Пароль"
                            value={login.password}
                            onChange={(e) =>
                                setLogin((s) => ({ ...s, password: e.target.value }))
                            }
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') doLogin();
                            }}
                        />
                        <Button
                            variant="contained"
                            size="large"
                            sx={{ height: 40 }}
                            onClick={doLogin}
                            disabled={loginDisabled}
                        >
                            Войти
                        </Button>
                        <Stack direction="row" spacing={1} justifyContent="center">
                            <Typography variant="body2" color="textSecondary">Нет аккаунта?</Typography>
                            <Typography
                                variant="body2"
                                fontWeight="medium"
                                onClick={toggleForm}
                                sx={{ cursor: "pointer", userSelect: "none" }}
                            >
                                Регистрация
                            </Typography>
                        </Stack>
                    </>
                ) : (
                    <>
                        <Typography variant="h6">Регистрация</Typography>
                        <TextField
                            size="small"
                            placeholder="Логин"
                            value={reg.username}
                            onChange={(e) =>
                                setReg((s) => ({ ...s, username: e.target.value }))
                            }
                            autoFocus
                        />
                        <TextField
                            size="small"
                            type="email"
                            placeholder="Email"
                            value={reg.email}
                            onChange={(e) =>
                                setReg((s) => ({ ...s, email: e.target.value }))
                            }
                        />
                        <TextField
                            size="small"
                            placeholder="Как показывать вас в ленте?"
                            value={reg.displayName}
                            onChange={(e) =>
                                setReg((s) => ({ ...s, displayName: e.target.value }))
                            }
                        />
                        <TextField
                            size="small"
                            type="password"
                            placeholder="Пароль"
                            value={reg.password}
                            onChange={(e) =>
                                setReg((s) => ({ ...s, password: e.target.value }))
                            }
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') doRegister();
                            }}
                        />
                        <Button
                            variant="contained"
                            size="large"
                            sx={{ height: 40 }}
                            onClick={doRegister}
                            disabled={regDisabled}
                        >
                            Зарегистрироваться
                        </Button>
                        <Stack direction="row" spacing={1} justifyContent="center">
                            <Typography variant="body2" color="textSecondary">Есть аккаунт?</Typography>
                            <Typography
                                variant="body2"
                                fontWeight="medium"
                                onClick={toggleForm}
                                sx={{ cursor: "pointer", userSelect: "none" }}
                            >
                                Вход
                            </Typography>
                        </Stack>
                    </>
                )}
            </Stack>
        </Paper>
    );
}