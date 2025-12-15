import React, { useState } from 'react';
import { apiBase } from '../api/api';
import { useAuth } from '../context/AuthContext';
import Status from '../components/Status';

export default function AuthPage({ active, onTab }) {
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

            onTab("account"); // Переключаем после успешного входа
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

            onTab("account"); // Переключаем после успешной регистрации
        } catch (e) {
            setStatusAuth('Ошибка регистрации: ' + e.message, 'error');
        }
    }

    const toggleForm = () => {
        setIsLogin(!isLogin); // Переключаем форму
    };

    if (!active) return null;

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageAuth">
            {isLogin ? (
                <>
                    <div className="studio-section-title">Вход</div>
                    <div className="form-row">
                        <label htmlFor="loginUsername">Логин</label>
                        <input
                            id="loginUsername"
                            type="text"
                            placeholder="username"
                            value={login.username}
                            onChange={(e) => setLogin((s) => ({ ...s, username: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="loginPassword">Пароль</label>
                        <input
                            id="loginPassword"
                            type="password"
                            placeholder="••••••••"
                            value={login.password}
                            onChange={(e) => setLogin((s) => ({ ...s, password: e.target.value }))}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') doLogin();
                            }}
                        />
                    </div>
                    <button className="full" id="btnLogin" onClick={doLogin}>
                        Войти
                    </button>
                </>
            ) : (
                <>
                    <div className="studio-section-title">Регистрация</div>
                    <div className="form-row">
                        <label htmlFor="regUsername">Логин</label>
                        <input
                            id="regUsername"
                            type="text"
                            placeholder="username"
                            value={reg.username}
                            onChange={(e) => setReg((s) => ({ ...s, username: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="regEmail">Email</label>
                        <input
                            id="regEmail"
                            type="email"
                            placeholder="you@example.com"
                            value={reg.email}
                            onChange={(e) => setReg((s) => ({ ...s, email: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="regDisplayName">Отображаемое имя</label>
                        <input
                            id="regDisplayName"
                            type="text"
                            placeholder="Как показывать вас в ленте?"
                            value={reg.displayName}
                            onChange={(e) => setReg((s) => ({ ...s, displayName: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="regPassword">Пароль</label>
                        <input
                            id="regPassword"
                            type="password"
                            placeholder="••••••••"
                            value={reg.password}
                            onChange={(e) => setReg((s) => ({ ...s, password: e.target.value }))}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') doRegister();
                            }}
                        />
                    </div>
                    <button className="full secondary" id="btnRegister" onClick={doRegister}>
                        Зарегистрироваться
                    </button>
                </>
            )}

            <div className="toggle-form">
                {isLogin ? (
                    <>Нет аккаунта? <span onClick={toggleForm}>Регистрация</span></>
                ) : (
                    <>Есть аккаунт? <span onClick={toggleForm}>Вход</span></>
                )}
            </div>

            <Status id="statusAuth" message={status.msg} type={status.type}/>
        </div>
    );
}