import { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import {useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { apiService } from '../../services/api';
import { Link } from 'react-router-dom';

interface RegisterFormInputs {
  username: string;
  email: string;
  password: string;
}

const Register = () => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const navigate = useNavigate();
  const {login } = useAuth();

  const userRef = useRef<HTMLInputElement>(null);
  const errRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<RegisterFormInputs>({
    mode: 'onChange',
  });

  useEffect(() => {
    userRef.current?.focus();
  }, []);

  useEffect(() => {
    setErrorMessage('');
  }, [errors.username, errors.email, errors.password]); 

  const onSubmit = async (data: RegisterFormInputs) => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const response = await apiService.register(data);
      login(response);
      reset();
      navigate('/dashboard');
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Registration failed',
      );
    } finally {
      setIsLoading(false);
      setSuccess(true);
    }
  };

  return (
    <>
    {success ? (
      <section className="mt-16 text-center">
        <h1 className="text-4xl text-orange-800">Success!</h1>
        <Link to={'/dashboard'} className="btn btn-primary mt-4">Go to Dashboard</Link>
      </section>
    ) : (
      <form
      onSubmit={handleSubmit(onSubmit)}
      className="mt-8 flex flex-col gap-4 lg:gap-6 max-w-[500px] mx-auto"
    >
      <div>
        <input
          {...register('username', {
            required: 'Username is required',
            pattern: {
              value: /^[a-zA-Z][a-zA-Z0-9-_]{3,23}$/,
              message:
                'Username must be 3-23 characters (letters, numbers, underscore only)',
            },
            minLength: {
              value: 3,
              message: 'Minimum of 3 characters',
            },
            maxLength: {
              value: 20,
              message: 'Maximum of 20 characters',
            },
          })}
          className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
            errors.username
              ? 'border-red-400 outline-red-400 focus:outline-red-400'
              : ''
          }`}
          placeholder="Username"
          name="username"
          autoComplete="username"
        />
        {errors.username && (
          <p className="text-red-500 text-sm mt-2 ml-2">
            {errors.username.message}
          </p>
        )}
      </div>
      <div>
        <input
          {...register('email', {
            required: 'Email is required',
            pattern: {
              value: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
              message:
                'Email must be 5-15 characters (letters, numbers, underscore only)',
            },
            minLength: {
              value: 5,
              message: 'Minimum 5 characters',
            },
            maxLength: {
              value: 20,
              message: 'Maximum 20 characters',
            },
          })}
          type="email"
          placeholder="Email"
          name="email"
          autoComplete="email"
          className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
            errors.email
              ? 'border-red-400 outline-red-400 focus:outline-red-400'
              : ''
          }`}
        />
        {errors.email && (
          <p className="text-red-500 text-sm mt-2 ml-2">
            {errors.email.message}
          </p>
        )}
      </div>
      <div>
        <input
          {...register('password', {
            required: 'Password is required',
            pattern: {
              value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,}$/,
              message: 'Password must be at least 6 characters and include uppercase, lowercase, number, and special character',
            },
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
        Register
      </button>
    </form>
    )}
    </>
  );
};

export default Register;
