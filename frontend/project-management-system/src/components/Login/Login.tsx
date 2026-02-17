import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { apiService } from '../../services/api';

interface LoginFormInputs {
  username: string;
  password: string;
}

const Login = () => {

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<LoginFormInputs>({
    mode: 'onChange',
  });

  const onSubmit = async (data: LoginFormInputs) => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const response = await apiService.login(data);
      login(response);
      reset();

      if(response.role === "ADMIN") {
        navigate('/admin/dashboard');
      } else {
        navigate('/dashboard');
      }
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Login failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="mt-8 flex flex-col
          gap-4 lg:gap-6 max-w-[500px] mx-auto"
    >
      <div>
        <input
          {...register('username', {
            required: 'Username is required',
            pattern: {
              value: /^[a-zA-Z0-9_]{5,15}$/,
              message:
                'Username must be 5-15 characters (letters, numbers, underscore only)',
            },
            minLength: {
              value: 3,
              message: '3 characters mininum',
            },
            maxLength: {
              value: 20,
              message: '20 characters maximum',
            },
          })}
          placeholder="Username"
          name="username"
          autoComplete="username"
          className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
            errors.username
              ? 'border-red-400 outline-red-400 focus:outline-red-400'
              : ''
          }`}
        />
        {errors.username && (
          <p className="text-red-500 text-sm mt-2 ml-2">
            {errors.username.message}
          </p>
        )}
      </div>
      <div>
        <input
          {...register('password', {
            required: 'Password is required',
            minLength: {
              value: 6,
              message: 'Password must be at least 6 characters',
            },
            maxLength: {
              value: 20,
              message: 'Password can be maximum of 20 characters',
            },
          })}
          type="password"
          placeholder="Password"
          name="password"
          autoComplete="current-password"
          className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
            errors.password
              ? 'border-red-400 outline-red-400 focus:outline-red-400'
              : ''
          }`}
        />
        {errors.password && (
          <p className="text-red-500 text-sm mt-2 ml-2">
            {errors.password.message}
          </p>
        )}
      </div>
      <button
        type="submit"
        className="bg-primary cursor-pointer p-2 text-white font-bold"
      >
        Login
      </button>
    </form>
  );
};

export default Login;
