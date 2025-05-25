import { useState } from "react";
import { useClient } from "@state/client";
import { useUser } from "@state/user";
import { useNavigate } from "react-router";

import { useToken } from "@state/token";

import styles from "./login.module.scss";

export default function LoginPage() {
  const navigate = useNavigate();

  const client = useClient((state) => state.client);
  const setUser = useUser((state) => state.update);
  const setToken = useToken((state) => state.update);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function login(e: React.MouseEvent<HTMLButtonElement, MouseEvent>) {
    e.preventDefault();

    client.auth
      .login(
        {
          username: username,
          password: password,
        },
        { secure: false }
      )
      .then((r) =>
        r.json().then((data) => {
          try {
            setUser({
              id: Number(data["id"]),
              username: data["username"],
              permissions: new Set<string>(),
            });

            setToken({
              accessToken: data["accessToken"],
              refreshToken: data["refreshToken"],
              tokenType: "Bearer",
              issueAtMS: Number(data["iat"]),
              expiresInMS: Number(data["expiresIn"]) * 1000,
            });

            navigate("/");
          } catch (e) {
            console.error(e);
          }
        })
      )
      .catch(async (e) => {
        switch (e.status) {
          case 401:
            alert("Login failed");
            break;
          default:
            alert(await e.json());
        }
      });
  }

  return (
    <>
      <div className={styles.root}>
        <div>ITMO PAI</div>
        <form>
          <div>
            <label>Username:</label>
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
          <div>
            <label>Password:</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button onClick={login}>Login</button>
        </form>
      </div>
    </>
  );
}
