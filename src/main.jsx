import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import './index.css'

import App from './App.jsx'
import { FeedPage } from "@/features/feed/FeedPage.jsx";

const router = createBrowserRouter([{
    path: "/",
    element: <App/>,
    children: [
        // Index route redirects to /feed
        { index: true, element: <FeedPage/> },
        // Explicit route for the feed
        { path: "feed", element: <FeedPage/> },
        // Upload page (requires auth)
        { path: "upload", element: null },
        // Profile page: Can show own profile (no ID) or a specific user's profile (with ID)
        { path: "profile", element: null }, // Route for own profile (if applicable logic exists in component)
        { path: "profile/:userId", element: null }, // Route for another user's profile
        // Authentication page (login/register)
        { path: "auth", element: null },
        // Moderation page (requires moderator/admin role)
        { path: "moderation", element: null },
        // Catch-all route for undefined paths, redirects to feed
        { path: "*", element: <FeedPage/> },
    ],
}]);

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <RouterProvider router={router}/>
    </StrictMode>,
)
