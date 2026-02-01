import { useRef, useState } from 'react';
import { useNavigate } from "react-router";
import { Button, Paper, Stack, TextField, Typography } from "@mui/material";

import { apiBase, getAvatarUrl } from '../api/api';
import { useAuth } from '../context/AuthContext';

function validateEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export default function AuthPage() {
    const { setAuth } = useAuth();
    const navigate = useNavigate();

    const [status, setStatus] = useState({ msg: '', type: '' });
    const [emailError, setEmailError] = useState('');
    const [regFieldError, setRegFieldError] = useState(''); // ошибка регистрации (внизу)
    const [loginError, setLoginError] = useState('');       // ошибка входа (внизу)

    const [login, setLogin] = useState({ username: '', password: '' });
    const [reg, setReg] = useState({ username: '', email: '', displayName: '', password: '' });
    const emailTimerRef = useRef(null);

    const [isLogin, setIsLogin] = useState(true); // Статус текущей вкладки (Вход / Регистрация)

    function setStatusAuth(msg = '', type = '') {
        setStatus({ msg, type });
    }

    async function doLogin() {
        setLoginError('');

        const username = login.username.trim();
        const password = login.password;

        if (!username || !password) {
            setLoginError('Введите логин и пароль');
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });

            if (!res.ok) {
                setLoginError('Ошибка входа');
                return;
            }

            const data = await res.json();
            setAuth(data.token, {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: data.roles ?? [],
                avatarUrl: getAvatarUrl(data.userId, true),
            });

            setLogin({ username: '', password: '' });
            navigate('/account/videos');
        } catch {
            setLoginError('Ошибка входа');
        }
    }

    async function doRegister() {
        setRegFieldError('');

        const username = reg.username.trim();
        const email = reg.email.trim();
        const displayName = reg.displayName.trim();
        const password = reg.password;

        if (!username || !email || !displayName || !password) {
            setRegFieldError('Заполните все поля регистрации');
            return;
        }

        if (emailError) return;

        try {
            const res = await fetch(apiBase + '/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, displayName, password }),
            });

            if (res.status === 400) {
                setRegFieldError('Имя пользователя уже занято');
                return;
            }

            if (!res.ok) throw new Error();

            const data = await res.json();
            setAuth(data.token, {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: data.roles ?? [],
                avatarUrl: getAvatarUrl(data.userId, true),
            });

            setReg({ username: '', email: '', displayName: '', password: '' });
            navigate('/account/videos');
        } catch {
            setRegFieldError('Ошибка регистрации');
        }
    }

    const toggleForm = () => setIsLogin(!isLogin);

    function handleEmailChange(e) {
        const value = e.target.value;

        setReg((s) => ({ ...s, email: value }));
        setEmailError('');

        if (emailTimerRef.current) {
            clearTimeout(emailTimerRef.current);
        }

        emailTimerRef.current = window.setTimeout(() => {
            if (value && !validateEmail(value)) {
                setEmailError('Неверный формат почты');
            }
        }, 300);
    }

    const loginDisabled =
        !login.username.trim() ||
        !login.password;

    const regDisabled =
        !reg.username.trim() ||
        !reg.email.trim() ||
        !reg.displayName.trim() ||
        !reg.password;

    return (
        <>
            <Stack component={Paper} direction="row" p={1}>
                <Typography variant="h6" px={1} py="1px">
                    {isLogin ? "Вход" : "Регистрация"}
                </Typography>
            </Stack>

            <Paper elevation={0} sx={{ flex: 1 }}>
                <Stack p={2} spacing={2} height={1}>
                    {isLogin ? (
                        <>
                            <TextField
                                size="small"
                                placeholder="Логин"
                                value={login.username}
                                onChange={(e) => {
                                    setLoginError('');
                                    setLogin((s) => ({ ...s, username: e.target.value }));
                                }}
                                autoFocus
                                error={!!loginError}
                            />
                            <TextField
                                size="small"
                                type="password"
                                placeholder="Пароль"
                                value={login.password}
                                onChange={(e) => {
                                    setLoginError('');
                                    setLogin((s) => ({ ...s, password: e.target.value }));
                                }}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter') doLogin();
                                }}
                                error={!!loginError}
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
                            <TextField
                                size="small"
                                placeholder="Логин"
                                value={reg.username}
                                onChange={(e) => {
                                    setRegFieldError('');
                                    setReg((s) => ({ ...s, username: e.target.value }));
                                }}
                                autoFocus
                                error={!!regFieldError}
                            />
                            <TextField
                                size="small"
                                type="email"
                                placeholder="Email"
                                value={reg.email}
                                onChange={handleEmailChange}
                                error={!!emailError}
                                helperText={emailError}
                            />
                            <TextField
                                size="small"
                                placeholder="Как показывать вас в ленте?"
                                value={reg.displayName}
                                onChange={(e) => {
                                    setRegFieldError('');
                                    setReg((s) => ({ ...s, displayName: e.target.value }));
                                }}
                                error={!!regFieldError}
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
                    {loginError && (
                        <Typography color="error" variant="body2" align="center">
                            {loginError}
                        </Typography>
                    )}
                    {regFieldError && (
                        <Typography color="error" variant="body2" align="center">
                            {regFieldError}
                        </Typography>
                    )}
                </Stack>
            </Paper>
        </>
    );
}