import { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { apiService } from '../../services/api';

interface RegisterFormInputs {
  username: string;
  email: string;
  password: string;
}

const Register = () => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const userRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    clearErrors,
  } = useForm<RegisterFormInputs>({
    mode: 'onChange',
    defaultValues: {
      username: '',
      email: '',
      password: '',
    },
  });

  // Reset form and clear errors when component mounts
  useEffect(() => {
    reset({
      username: '',
      email: '',
      password: '',
    });
    clearErrors();
    userRef.current?.focus();
  }, [reset, clearErrors]);

  useEffect(() => {
    setErrorMessage('');
  }, [errors.username, errors.email, errors.password]);

  const onSubmit = async (data: RegisterFormInputs) => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      // Register the user but DON'T log them in
      await apiService.register(data);
      reset();
      setSuccess(true); // Show success message
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Registration failed',
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {success ? (
        <section className="mt-16 text-center">
          <div className="max-w-md mx-auto bg-green-50 border border-green-200 rounded-lg p-8">
            <svg
              className="w-16 h-16 mx-auto text-green-500 mb-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <h1 className="text-3xl font-bold text-green-800 mb-2">
              Registration Successful!
            </h1>
            <p className="text-gray-600 mb-6">
              Your account has been created successfully. Please sign in to
              continue.
            </p>
            <Link to="/login" className="btn btn-primary">
              Sign In
            </Link>
          </div>
        </section>
      ) : (
        <>
          {errorMessage && (
            <div className="alert alert-error max-w-[500px] mx-auto mt-4">
              <span>{errorMessage}</span>
            </div>
          )}

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="mt-8 flex flex-col gap-4 lg:gap-6 max-w-[500px] mx-auto"
          >
            <div>
              <input
                {...register('username', {
                  required: 'Username is required',
                  pattern: {
                    value: /^[a-zA-Z0-9_-]{3,20}$/,
                    message:
                      'Username must be 3-20 characters (letters, numbers, underscore, hyphen only)',
                  },
                })}
                className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
                  errors.username
                    ? 'border-2 border-red-400 outline-red-400 focus:outline-red-400'
                    : 'border border-gray-300'
                }`}
                placeholder="Username"
                autoComplete="off"
                disabled={isLoading}
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
                    message: 'Please enter a valid email address',
                  },
                })}
                type="email"
                placeholder="Email"
                autoComplete="off"
                disabled={isLoading}
                className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
                  errors.email
                    ? 'border-2 border-red-400 outline-red-400 focus:outline-red-400'
                    : 'border border-gray-300'
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
                  minLength: {
                    value: 6,
                    message: 'Password must be at least 6 characters',
                  },
                  maxLength: {
                    value: 20,
                    message: 'Password can be maximum of 20 characters',
                  },
                  pattern: {
                    value:
                      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,}$/,
                    message:
                      'Password must include uppercase, lowercase, number, and special character',
                  },
                })}
                type="password"
                placeholder="Password"
                autoComplete="new-password"
                disabled={isLoading}
                className={`p-2 outline-2 rounded focus:outline-blue-400 w-full ${
                  errors.password
                    ? 'border-2 border-red-400 outline-red-400 focus:outline-red-400'
                    : 'border border-gray-300'
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
              disabled={isLoading}
              className="bg-primary cursor-pointer p-2 text-white font-bold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Registering...' : 'Register'}
            </button>
          </form>
        </>
      )}
    </>
  );
};

export default Register;
